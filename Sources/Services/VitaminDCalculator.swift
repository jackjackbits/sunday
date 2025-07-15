import Foundation
import Combine
import HealthKit
import UserNotifications
import WidgetKit
import UIKit

enum ClothingLevel: Int, CaseIterable {
    case none = -1
    case minimal = 0
    case light = 1
    case moderate = 2
    case heavy = 3
    
    var description: String {
        switch self {
        case .none: return "Nude!"
        case .minimal: return "Minimal (swimwear)"
        case .light: return "Light (shorts & t-shirt)"
        case .moderate: return "Moderate (long sleeves)"
        case .heavy: return "Heavy (fully covered)"
        }
    }
    
    var exposureFactor: Double {
        switch self {
        case .none: return 1.0
        case .minimal: return 0.80
        case .light: return 0.40
        case .moderate: return 0.15
        case .heavy: return 0.05
        }
    }
}

enum SkinType: Int, CaseIterable {
    case type1 = 1
    case type2 = 2
    case type3 = 3
    case type4 = 4
    case type5 = 5
    case type6 = 6
    
    var description: String {
        switch self {
        case .type1: return "Very fair"
        case .type2: return "Fair"
        case .type3: return "Light"
        case .type4: return "Medium"
        case .type5: return "Dark"
        case .type6: return "Very dark"
        }
    }
    
    var vitaminDFactor: Double {
        switch self {
        case .type1: return 1.25   // Very fair produces more
        case .type2: return 1.1    // Fair produces more
        case .type3: return 1.0    // Light skin is reference
        case .type4: return 0.7    // Medium skin
        case .type5: return 0.4    // Dark skin
        case .type6: return 0.2    // Very dark skin
        }
    }
}

class VitaminDCalculator: ObservableObject {
    @Published var isInSun = false
    @Published var clothingLevel: ClothingLevel = .light {
        didSet {
            UserDefaults.standard.set(clothingLevel.rawValue, forKey: "preferredClothingLevel")
        }
    }
    @Published var skinType: SkinType = .type3 {
        didSet {
            UserDefaults.standard.set(skinType.rawValue, forKey: "userSkinType")
            // Check if manually selected type matches HealthKit value
            if !isSettingFromHealth {
                checkIfMatchesHealthKitSkinType()
            }
        }
    }
    @Published var currentVitaminDRate: Double = 0.0
    @Published var sessionVitaminD: Double = 0.0
    @Published var sessionStartTime: Date?
    @Published var skinTypeFromHealth = false
    @Published var cumulativeMEDFraction: Double = 0.0
    @Published var userAge: Int = 30 {
        didSet {
            UserDefaults.standard.set(userAge, forKey: "userAge")
        }
    }
    @Published var ageFromHealth = false
    @Published var currentUVQualityFactor: Double = 1.0
    @Published var currentAdaptationFactor: Double = 1.0
    
    private var timer: Timer?
    private var lastUV: Double = 0.0
    private var healthManager: HealthManager?
    private var isSettingFromHealth = false
    private weak var uvService: UVService?
    private var healthKitSkinType: SkinType?
    private var lastUpdateTime: Date?
    private let sharedDefaults = UserDefaults(suiteName: "group.sunday.widget")
    private var appActiveObserver: NSObjectProtocol?
    private var appBackgroundObserver: NSObjectProtocol?
    private var wasTrackingBeforeBackground = false
    
    // UV response curve parameters
    private let uvHalfMax = 4.0  // UV index for 50% vitamin D synthesis rate (more linear)
    private let uvMaxFactor = 3.0 // Maximum multiplication factor at high UV
    
    init() {
        loadUserPreferences()
        setupAppLifecycleObservers()
    }
    
    deinit {
        if let observer = appActiveObserver {
            NotificationCenter.default.removeObserver(observer)
        }
        if let observer = appBackgroundObserver {
            NotificationCenter.default.removeObserver(observer)
        }
    }
    
    func setHealthManager(_ healthManager: HealthManager) {
        self.healthManager = healthManager
        checkHealthKitSkinType()
        checkHealthKitAge()
        updateAdaptationFactor()
    }
    
    func setUVService(_ uvService: UVService) {
        self.uvService = uvService
    }
    
    private func getSafeMinutes() -> Int {
        guard let uvService = uvService else { return 60 }
        return uvService.burnTimeMinutes[skinType.rawValue] ?? 60
    }
    
    private func loadUserPreferences() {
        if let savedClothingLevel = UserDefaults.standard.object(forKey: "preferredClothingLevel") as? Int,
           let clothing = ClothingLevel(rawValue: savedClothingLevel) {
            clothingLevel = clothing
        }
        
        if let savedSkinType = UserDefaults.standard.object(forKey: "userSkinType") as? Int,
           let skin = SkinType(rawValue: savedSkinType) {
            skinType = skin
        }
        
        if let savedAge = UserDefaults.standard.object(forKey: "userAge") as? Int {
            userAge = savedAge
        }
    }
    
    private func setupAppLifecycleObservers() {
        appBackgroundObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.didEnterBackgroundNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            guard let self = self else { return }
            // Save tracking state and pause timer
            self.wasTrackingBeforeBackground = self.isInSun
            if self.isInSun {
                self.timer?.invalidate()
                self.timer = nil
            }
        }
        
        appActiveObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.didBecomeActiveNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            guard let self = self else { return }
            // Resume timer if was tracking
            if self.wasTrackingBeforeBackground && self.isInSun && self.timer == nil {
                // Resume with 1-second timer
                self.timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
                    guard let self = self else { return }
                    let currentUV = self.lastUV
                    self.updateVitaminD(uvIndex: currentUV)
                    self.updateMEDExposure(uvIndex: currentUV)
                }
                // Update immediately
                self.updateVitaminD(uvIndex: self.lastUV)
                self.updateMEDExposure(uvIndex: self.lastUV)
            }
        }
    }
    
    func startSession(uvIndex: Double) {
        guard isInSun else { return }
        
        sessionStartTime = Date()
        sessionVitaminD = 0.0
        cumulativeMEDFraction = 0.0
        lastUV = uvIndex
        lastUpdateTime = Date()
        
        // Update every second for real-time display
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            // Use the current UV from UVService, not lastUV
            let currentUV = self.lastUV
            self.updateVitaminD(uvIndex: currentUV)
            self.updateMEDExposure(uvIndex: currentUV)
        }
        
        updateVitaminDRate(uvIndex: uvIndex)
    }
    
    func stopSession() {
        timer?.invalidate()
        timer = nil
        sessionStartTime = nil
        cumulativeMEDFraction = 0.0
        
        // Cancel any pending burn warnings
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["burnWarning"])
        
        // Update widget data
        updateWidgetData()
    }
    
    func updateUV(_ uvIndex: Double) {
        lastUV = uvIndex
        updateVitaminDRate(uvIndex: uvIndex)
    }
    
    private func updateVitaminDRate(uvIndex: Double) {
        // Always calculate the rate to show potential vitamin D gain
        // Base rate: 21000 IU/hr for Type 3 skin with minimal clothing (80% exposure)
        // Conservative estimate within research range of 20,000-40,000 IU/hr
        // Full body exposure can reach 30,000-40,000 IU/hr in optimal conditions
        let baseRate = 21000.0
        
        // UV factor: Michaelis-Menten-like saturation curve
        // More accurate representation of vitamin D synthesis kinetics
        // UV 0 = 0x, UV 3 = 1.25x (50% of max), UV 12 = 2x, UV∞ → 2.5x
        let uvFactor = (uvIndex * uvMaxFactor) / (uvHalfMax + uvIndex)
        
        // Exposure based on clothing coverage
        let exposureFactor = clothingLevel.exposureFactor
        
        // Skin type affects vitamin D synthesis efficiency
        let skinFactor = skinType.vitaminDFactor
        
        // Age factor: vitamin D synthesis decreases with age
        // ~25% synthesis at age 70 compared to age 20
        let ageFactor: Double
        if userAge <= 20 {
            ageFactor = 1.0
        } else if userAge >= 70 {
            ageFactor = 0.25
        } else {
            // Linear decrease: lose ~1% per year after age 20
            ageFactor = max(0.25, 1.0 - Double(userAge - 20) * 0.01)
        }
        
        // Calculate UV quality factor based on time of day
        currentUVQualityFactor = calculateUVQualityFactor()
        
        // Final calculation: base * UV * clothing * skin type * age * quality * adaptation
        currentVitaminDRate = baseRate * uvFactor * exposureFactor * skinFactor * ageFactor * currentUVQualityFactor * currentAdaptationFactor
        
        // Update widget with new rate
        updateWidgetData()
    }
    
    private func updateVitaminD(uvIndex: Double) {
        guard isInSun else { return }
        
        // Always recalculate rate with current UV to ensure accuracy
        updateVitaminDRate(uvIndex: uvIndex)
        
        // Calculate actual time elapsed since last update (should be ~1 second)
        let now = Date()
        let elapsed = lastUpdateTime.map { now.timeIntervalSince($0) } ?? 1.0
        lastUpdateTime = now
        
        // Add vitamin D based on actual elapsed time
        sessionVitaminD += currentVitaminDRate * (elapsed / 3600.0)
        
        // Update widget data
        updateWidgetData()
    }
    
    func toggleSunExposure(uvIndex: Double) {
        isInSun.toggle()
        
        if isInSun {
            startSession(uvIndex: uvIndex)
        } else {
            stopSession()
        }
    }
    
    private func checkHealthKitSkinType() {
        healthManager?.getFitzpatrickSkinType { [weak self] hkSkinType in
            guard let self = self, let hkSkinType = hkSkinType else { return }
            
            // Map HealthKit Fitzpatrick skin type to our SkinType enum
            let mappedSkinType: SkinType?
            switch hkSkinType {
            case .I:
                mappedSkinType = .type1
            case .II:
                mappedSkinType = .type2
            case .III:
                mappedSkinType = .type3
            case .IV:
                mappedSkinType = .type4
            case .V:
                mappedSkinType = .type5
            case .VI:
                mappedSkinType = .type6
            case .notSet:
                mappedSkinType = nil
            @unknown default:
                mappedSkinType = nil
            }
            
            // Store the HealthKit skin type for comparison
            self.healthKitSkinType = mappedSkinType
            
            // If we got a valid skin type from Health, use it
            if let mappedSkinType = mappedSkinType {
                self.isSettingFromHealth = true
                self.skinType = mappedSkinType
                self.skinTypeFromHealth = true
                self.isSettingFromHealth = false
            } else {
                self.skinTypeFromHealth = false
            }
        }
    }
    
    private func checkHealthKitAge() {
        healthManager?.getAge { [weak self] age in
            guard let self = self, let age = age else { return }
            
            self.userAge = age
            self.ageFromHealth = true
            
            // Recalculate vitamin D rate with new age
            self.updateVitaminDRate(uvIndex: self.lastUV)
        }
    }
    
    private func checkIfMatchesHealthKitSkinType() {
        // If user manually selects the same skin type as HealthKit, show the heart icon
        if let healthKitType = healthKitSkinType, healthKitType == skinType {
            skinTypeFromHealth = true
        } else {
            skinTypeFromHealth = false
        }
    }
    
    private func updateMEDExposure(uvIndex: Double) {
        guard isInSun, uvIndex > 0 else { return }
        
        // MED values at UV 1 (must match UVService values)
        let medTimesAtUV1: [Int: Double] = [
            1: 150.0,  // Type I
            2: 250.0,  // Type II
            3: 425.0,  // Type III
            4: 600.0,  // Type IV
            5: 850.0,  // Type V
            6: 1100.0  // Type VI
        ]
        
        guard let medTimeAtUV1 = medTimesAtUV1[skinType.rawValue] else { return }
        
        // Calculate MED per second at current UV
        let medMinutesAtCurrentUV = medTimeAtUV1 / uvIndex
        let medFractionPerSecond = 1.0 / (medMinutesAtCurrentUV * 60.0)
        
        // Accumulate MED exposure
        cumulativeMEDFraction += medFractionPerSecond
        
        // Check if approaching burn threshold (80% MED)
        if cumulativeMEDFraction >= 0.8 && cumulativeMEDFraction < 0.81 {
            // Send notification that user is approaching burn limit
            scheduleImmediateBurnWarning()
        }
    }
    
    private func scheduleImmediateBurnWarning() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            
            let content = UNMutableNotificationContent()
            content.title = "🔥 Approaching burn limit!"
            content.body = "You've reached 80% of your burn threshold. Consider seeking shade."
            content.sound = .default
            
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
            let request = UNNotificationRequest(identifier: "burnWarning", content: content, trigger: trigger)
            
            UNUserNotificationCenter.current().add(request)
        }
    }
    
    private func calculateUVQualityFactor() -> Double {
        let calendar = Calendar.current
        let now = Date()
        let hour = calendar.component(.hour, from: now)
        let minute = calendar.component(.minute, from: now)
        
        // Convert to decimal hours
        let timeDecimal = Double(hour) + Double(minute) / 60.0
        
        // Solar noon approximation (varies by location, but ~13:00 is reasonable)
        let solarNoon = 13.0
        
        // Hours from solar noon
        let hoursFromNoon = abs(timeDecimal - solarNoon)
        
        // UV-B effectiveness decreases from solar noon
        // Peak quality 10 AM - 3 PM (strong UV-B window)
        // More gradual reduction than previously modeled
        let qualityFactor = exp(-hoursFromNoon * 0.2)
        
        // Ensure minimum quality during daylight hours
        return max(0.1, min(1.0, qualityFactor))
    }
    
    private func updateAdaptationFactor() {
        healthManager?.getVitaminDHistory(days: 7) { [weak self] history in
            guard let self = self else { return }
            
            // Calculate average daily exposure over past 7 days
            let totalDays = 7.0
            let totalVitaminD = history.values.reduce(0, +)
            let averageDailyExposure = totalVitaminD / totalDays
            
            // Adaptation factor based on recent exposure
            // Low exposure (0-1000 IU/day avg) → 0.8x
            // Moderate exposure (5000 IU/day avg) → 1.0x  
            // High exposure (10000+ IU/day avg) → 1.2x
            let adaptationFactor: Double
            if averageDailyExposure < 1000 {
                adaptationFactor = 0.8
            } else if averageDailyExposure >= 10000 {
                adaptationFactor = 1.2
            } else {
                // Linear interpolation between 0.8 and 1.2
                adaptationFactor = 0.8 + (averageDailyExposure - 1000) / 9000 * 0.4
            }
            
            self.currentAdaptationFactor = adaptationFactor
            
            // Recalculate rate with new adaptation factor
            self.updateVitaminDRate(uvIndex: self.lastUV)
        }
    }
    
    private func updateWidgetData() {
        guard let uvService = uvService else { return }
        
        sharedDefaults?.set(uvService.currentUV, forKey: "currentUV")
        sharedDefaults?.set(isInSun, forKey: "isTracking")
        sharedDefaults?.set(currentVitaminDRate, forKey: "vitaminDRate")
        
        // Calculate today's total including current session
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: Date())
        let endOfDay = calendar.date(byAdding: .day, value: 1, to: startOfDay)!
        
        healthManager?.readVitaminDIntake(from: startOfDay, to: endOfDay) { [weak self] total, error in
            guard let self = self else { return }
            let todaysTotal = total + self.sessionVitaminD
            self.sharedDefaults?.set(todaysTotal, forKey: "todaysTotal")
            
            // Trigger widget update
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
}
