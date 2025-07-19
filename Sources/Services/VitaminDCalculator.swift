import Foundation
import Combine
import HealthKit
import UserNotifications
import WidgetKit
import UIKit
import ActivityKit

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
        case .light: return "Light (shorts, tee)"
        case .moderate: return "Moderate (pants, tee)"
        case .heavy: return "Heavy (pants, sleeves)"
        }
    }

    var exposureFactor: Double {
        switch self {
        case .none: return 1.0
        case .minimal: return 0.80
        case .light: return 0.50
        case .moderate: return 0.30
        case .heavy: return 0.10
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
        case .type1: return 1.25
        case .type2: return 1.1
        case .type3: return 1.0
        case .type4: return 0.7
        case .type5: return 0.4
        case .type6: return 0.2
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
    @Published var userAge: Int? = nil {
        didSet {
            if let age = userAge {
                UserDefaults.standard.set(age, forKey: "userAge")
            } else {
                UserDefaults.standard.removeObject(forKey: "userAge")
            }
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
    private weak var locationManager: LocationManager?
    private var healthKitSkinType: SkinType?
    private var lastUpdateTime: Date?
    private let sharedDefaults = UserDefaults(suiteName: "group.jh.sunday.widget")
    private var activity: Activity<SundayActivityAttributes>? = nil

    private let uvHalfMax = 4.0
    private let uvMaxFactor = 3.0

    init() {
        loadUserPreferences()
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
    
    func setLocationManager(_ locationManager: LocationManager) {
        self.locationManager = locationManager
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
        } else {
            userAge = nil
        }
    }

    func startSession(uvIndex: Double) {
        guard isInSun else { return }

        sessionStartTime = Date()
        sessionVitaminD = 0.0
        cumulativeMEDFraction = 0.0
        lastUV = uvIndex
        lastUpdateTime = Date()

        locationManager?.startHighFrequencyUpdates()

        if activity == nil {
            let attributes = SundayActivityAttributes()
            let initialState = SundayActivityAttributes.ContentState(sessionDuration: 0, currentUVIndex: uvIndex, sessionVitaminD: 0)
            do {
                let activity = try Activity<SundayActivityAttributes>.request(
                    attributes: attributes,
                    contentState: initialState,
                    pushType: nil)
                self.activity = activity
            } catch (let error) {
                print("Error starting Live Activity: \(error.localizedDescription)")
            }
        }

        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.updateLiveActivity()
        }

        updateVitaminDRate(uvIndex: uvIndex)
    }

    func stopSession() {
        timer?.invalidate()
        timer = nil
        sessionStartTime = nil
        cumulativeMEDFraction = 0.0

        locationManager?.stopHighFrequencyUpdates()

        Task {
            await activity?.end(dismissalPolicy: .immediate)
            self.activity = nil
        }

        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["burnWarning"])
        updateWidgetData()
    }

    func updateUV(_ uvIndex: Double) {
        lastUV = uvIndex
        updateVitaminDRate(uvIndex: uvIndex)
    }

    func updateLiveActivity() {
        guard let uvService = self.uvService else { return }
        self.updateVitaminD(uvIndex: uvService.currentUV)
        self.updateMEDExposure(uvIndex: uvService.currentUV)
    }

    private func updateVitaminDRate(uvIndex: Double) {
        let baseRate = 21000.0
        let uvFactor = (uvIndex * uvMaxFactor) / (uvHalfMax + uvIndex)
        let exposureFactor = clothingLevel.exposureFactor
        let skinFactor = skinType.vitaminDFactor
        let ageFactor: Double
        if let age = userAge {
            if age <= 20 {
                ageFactor = 1.0
            } else if age >= 70 {
                ageFactor = 0.25
            } else {
                ageFactor = max(0.25, 1.0 - Double(age - 20) * 0.01)
            }
        } else {
            ageFactor = 1.0
        }
        currentUVQualityFactor = calculateUVQualityFactor()
        currentVitaminDRate = baseRate * uvFactor * exposureFactor * skinFactor * ageFactor * currentUVQualityFactor * currentAdaptationFactor
        updateWidgetData()
    }

    private func updateVitaminD(uvIndex: Double) {
        guard isInSun, let startTime = sessionStartTime else { return }
        updateVitaminDRate(uvIndex: uvIndex)
        let now = Date()
        let elapsed = lastUpdateTime.map { now.timeIntervalSince($0) } ?? 1.0
        lastUpdateTime = now
        sessionVitaminD += currentVitaminDRate * (elapsed / 3600.0)
        let contentState = SundayActivityAttributes.ContentState(
            sessionDuration: now.timeIntervalSince(startTime),
            currentUVIndex: uvIndex,
            sessionVitaminD: sessionVitaminD
        )
        Task {
            await activity?.update(using: contentState)
        }
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
    
    func addManualEntry(amount: Double) {
        sessionVitaminD += amount
        updateWidgetData()
    }

    func calculateVitaminD(uvIndex: Double, exposureMinutes: Double, skinType: SkinType, clothingLevel: ClothingLevel) -> Double {
        let baseRate = 21000.0
        let uvFactor = (uvIndex * uvMaxFactor) / (uvHalfMax + uvIndex)
        let exposureFactor = clothingLevel.exposureFactor
        let skinFactor = skinType.vitaminDFactor
        let ageFactor: Double
        if let age = userAge {
            if age <= 20 {
                ageFactor = 1.0
            } else if age >= 70 {
                ageFactor = 0.25
            } else {
                ageFactor = max(0.25, 1.0 - Double(age - 20) * 0.01)
            }
        } else {
            ageFactor = 1.0
        }

        let adaptationFactor = currentAdaptationFactor
        let hourlyRate = baseRate * uvFactor * exposureFactor * skinFactor * ageFactor * adaptationFactor
        return hourlyRate * (exposureMinutes / 60.0)
    }

    private func checkHealthKitSkinType() {
        healthManager?.getFitzpatrickSkinType { [weak self] hkSkinType in
            guard let self = self, let hkSkinType = hkSkinType else { return }

            let mappedSkinType: SkinType?
            switch hkSkinType {
            case .I: mappedSkinType = .type1
            case .II: mappedSkinType = .type2
            case .III: mappedSkinType = .type3
            case .IV: mappedSkinType = .type4
            case .V: mappedSkinType = .type5
            case .VI: mappedSkinType = .type6
            case .notSet: mappedSkinType = nil
            @unknown default: mappedSkinType = nil
            }

            self.healthKitSkinType = mappedSkinType
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
            guard let self = self else { return }
            if let age = age {
                self.userAge = age
                self.ageFromHealth = true
            } else {
                self.userAge = nil
                self.ageFromHealth = false
            }
            self.updateVitaminDRate(uvIndex: self.lastUV)
        }
    }

    private func checkIfMatchesHealthKitSkinType() {
        if let healthKitType = healthKitSkinType, healthKitType == skinType {
            skinTypeFromHealth = true
        } else {
            skinTypeFromHealth = false
        }
    }

    private func updateMEDExposure(uvIndex: Double) {
        guard isInSun, uvIndex > 0 else { return }

        let medTimesAtUV1: [Int: Double] = [1: 150.0, 2: 250.0, 3: 425.0, 4: 600.0, 5: 850.0, 6: 1100.0]
        guard let medTimeAtUV1 = medTimesAtUV1[skinType.rawValue] else { return }

        let medMinutesAtCurrentUV = medTimeAtUV1 / uvIndex
        let medFractionPerSecond = 1.0 / (medMinutesAtCurrentUV * 60.0)
        cumulativeMEDFraction += medFractionPerSecond

        if cumulativeMEDFraction >= 0.8 && cumulativeMEDFraction < 0.81 {
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
        let timeDecimal = Double(hour) + Double(minute) / 60.0
        let solarNoon = 13.0
        let hoursFromNoon = abs(timeDecimal - solarNoon)
        let qualityFactor = exp(-hoursFromNoon * 0.2)
        return max(0.1, min(1.0, qualityFactor))
    }

    private func updateAdaptationFactor() {
        healthManager?.getVitaminDHistory(days: 7) { [weak self] history in
            guard let self = self else { return }
            let totalDays = 7.0
            let totalVitaminD = history.values.reduce(0, +)
            let averageDailyExposure = totalVitaminD / totalDays
            let adaptationFactor: Double
            if averageDailyExposure < 1000 {
                adaptationFactor = 0.8
            } else if averageDailyExposure >= 10000 {
                adaptationFactor = 1.2
            } else {
                adaptationFactor = 0.8 + (averageDailyExposure - 1000) / 9000 * 0.4
            }
            self.currentAdaptationFactor = adaptationFactor
            self.updateVitaminDRate(uvIndex: self.lastUV)
        }
    }

    private func updateWidgetData() {
        guard let uvService = uvService else { return }
        sharedDefaults?.set(uvService.currentUV, forKey: "currentUV")
        sharedDefaults?.set(isInSun, forKey: "isTracking")
        sharedDefaults?.set(currentVitaminDRate, forKey: "vitaminDRate")
        let calendar = Calendar.current
        let startOfDay = calendar.startOfDay(for: Date())
        let endOfDay = calendar.date(byAdding: .day, value: 1, to: startOfDay)!
        healthManager?.readVitaminDIntake(from: startOfDay, to: endOfDay) { [weak self] total, error in
            guard let self = self else { return }
            let todaysTotal = total + self.sessionVitaminD
            self.sharedDefaults?.set(todaysTotal, forKey: "todaysTotal")
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
}