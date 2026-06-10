import Foundation
import CoreLocation
import Combine
import UserNotifications
import SwiftData
import WidgetKit
import OSLog

struct OpenMeteoResponse: Codable {
    let daily: DailyData
    let hourly: HourlyData?
    
    struct DailyData: Codable {
        let time: [String]
        let uvIndexMax: [Double]
        let uvIndexClearSkyMax: [Double]?
        let sunrise: [String]
        let sunset: [String]
        
        enum CodingKeys: String, CodingKey {
            case time
            case uvIndexMax = "uv_index_max"
            case uvIndexClearSkyMax = "uv_index_clear_sky_max"
            case sunrise
            case sunset
        }
    }
    
    struct HourlyData: Codable {
        let time: [String]
        let uvIndex: [Double]
        let cloudCover: [Double]?
        
        enum CodingKeys: String, CodingKey {
            case time
            case uvIndex = "uv_index"
            case cloudCover = "cloud_cover"
        }
    }
}

class UVService: ObservableObject {
    private static let logger = Logger(subsystem: "it.sunday.app", category: "UVService")
    #if DEBUG
    private let signposter = OSSignposter(subsystem: "it.sunday.app", category: "Networking")
    #endif
    @Published var currentUV: Double = 0.0
    @Published var maxUV: Double = 0.0
    @Published var tomorrowMaxUV: Double = 0.0
    @Published var isLoading = false
    @Published var lastError: String?
    @Published var burnTimeMinutes: [Int: Int] = [:]
    @Published var todaySunrise: Date?
    @Published var todaySunset: Date?
    @Published var tomorrowSunrise: Date?
    @Published var tomorrowSunset: Date?
    @Published var currentAltitude: Double = 0.0
    @Published var uvMultiplier: Double = 1.0
    @Published var currentCloudCover: Double = 0.0
    /// Non-nil when the user has manually overridden the cloud cover (0–100).
    @Published var cloudCoverOverride: Double? = nil
    @Published var currentMoonPhase: Double = 0.0
    @Published var currentMoonPhaseName: String = ""
    @Published var isVitaminDWinter = false
    @Published var currentLatitude: Double = 0.0
    @Published var isOfflineMode = false
    @Published var lastSuccessfulUpdate: Date?
    @Published var hasNoData = false
    private var lastMoonPhaseUpdate: Date?
    private var modelContext: ModelContext?
    private var lastRetryLocation: CLLocation?
    private var networkMonitor: NetworkMonitor?
    private var networkCancellable: AnyCancellable?
    /// UV and cloud cover values as received from the API (before any user override).
    private var apiUV: Double = 0.0
    private var apiCloudCover: Double = 0.0

    // MARK: Cloud Cover Override

    /// Non-linear cloud transmission lookup based on WMO empirical data.
    /// The Open-Meteo UV model is not linear — thick cloud cover disproportionately
    /// blocks UVB. These values match observed UV attenuation at 550 nm (UVB range).
    ///   0% → 1.00  (full clear sky)
    ///  25% → 0.90  (thin scattered cloud, little effect)
    ///  50% → 0.72  (broken cloud, meaningful reduction)
    ///  75% → 0.45  (heavy overcast, large reduction)
    /// 100% → 0.17  (dense overcast / rain, but UV still penetrates)
    private func cloudTransmission(_ cloudPercent: Double) -> Double {
        switch cloudPercent {
        case 0:   return 1.00
        case 25:  return 0.90
        case 50:  return 0.72
        case 75:  return 0.45
        case 100: return 0.17
        default:
            // Interpolate between the nearest two breakpoints
            let breakpoints: [(Double, Double)] = [(0,1.00),(25,0.90),(50,0.72),(75,0.45),(100,0.17)]
            for i in 0..<breakpoints.count - 1 {
                let (x0, y0) = breakpoints[i]
                let (x1, y1) = breakpoints[i + 1]
                if cloudPercent <= x1 {
                    let t = (cloudPercent - x0) / (x1 - x0)
                    return y0 + t * (y1 - y0)
                }
            }
            return 0.17
        }
    }

    /// Apply a user-selected cloud cover override (0, 25, 50, 75, or 100).
    /// Back-calculates clear-sky UV from the API value using the same transmission
    /// table, then applies the override's factor — self-consistent per hour.
    /// e.g. at 59% cloud / UV 8.2: clearSky = 8.2 / 0.623 ≈ 13.2; at 0% → 13.2
    func applyCloudOverride(_ percent: Double) {
        let currentTransmission = cloudTransmission(apiCloudCover)
        guard currentTransmission > 0 else { return }
        let clearSkyUV = apiUV / currentTransmission
        currentUV = clearSkyUV * cloudTransmission(percent)
        currentCloudCover = percent
        cloudCoverOverride = percent
    }

    /// Remove the override and restore API values.
    func clearCloudOverride() {
        guard cloudCoverOverride != nil else { return }
        currentUV = apiUV
        currentCloudCover = apiCloudCover
        cloudCoverOverride = nil
    }

    var shouldShowTomorrowTimes: Bool {
        guard let todaySunset = todaySunset else { return false }
        let now = Date()
        let calendar = Calendar.current
        // Show tomorrow's times if we're past sunset but not yet midnight
        return now > todaySunset && calendar.isDateInToday(now)
    }
    
    var displaySunrise: Date? {
        shouldShowTomorrowTimes ? tomorrowSunrise : todaySunrise
    }
    
    var displaySunset: Date? {
        shouldShowTomorrowTimes ? tomorrowSunset : todaySunset
    }
    
    var displayMaxUV: Double {
        shouldShowTomorrowTimes ? tomorrowMaxUV : maxUV
    }
    
    private var cancellables = Set<AnyCancellable>()
    private var notificationScheduled = false
    
    func setModelContext(_ context: ModelContext) {
        self.modelContext = context
    }
    
    func setNetworkMonitor(_ monitor: NetworkMonitor) {
        self.networkMonitor = monitor
        
        // Observe network changes
        networkCancellable = monitor.$isConnected
            .removeDuplicates()
            .receive(on: DispatchQueue.main)
            .sink { [weak self] isConnected in
                guard let self = self else { return }
                
                if isConnected && self.isOfflineMode {
                    // Clear offline mode immediately
                    self.isOfflineMode = false
                    // Network is back, fetch fresh data
                    if let location = self.lastRetryLocation {
                        self.fetchUVData(for: location)
                    }
                } else if !isConnected && !self.isOfflineMode {
                    // Network disconnected, switch to offline mode if we have data
                    if !self.hasNoData {
                        self.isOfflineMode = true
                    }
                }
            }
    }
    
    func fetchUVData(for location: CLLocation) {
        isLoading = true
        lastError = nil
        
        // Save location for potential retry
        lastRetryLocation = location
        
        let latitude = location.coordinate.latitude
        let longitude = location.coordinate.longitude
        let altitude = location.altitude // Get altitude from location
        
        // Store latitude for vitamin D winter calculation
        currentLatitude = abs(latitude)
        
        // Always update altitude from GPS, even if network fails
        let validAltitude = altitude >= 0 ? altitude : 0
        currentAltitude = validAltitude
        let _ = validAltitude / 1000.0
        // Open-Meteo applies elevation when provided via `elevation`.
        // Keep uvMultiplier for reference but do not apply it to API UV values to avoid double counting.
        uvMultiplier = 1.0
        #if DEBUG
        Self.logger.debug("Fetch UV start lat=\(latitude, privacy: .public) lon=\(longitude, privacy: .public) elev=\(validAltitude, privacy: .public)m")
        let fetchInterval = signposter.beginInterval("FetchUV")
        #endif
        
        // Share with widget
        let sharedDefaults = UserDefaults(suiteName: "group.sunday.widget")
        sharedDefaults?.set(validAltitude, forKey: "currentAltitude")
        sharedDefaults?.set(uvMultiplier, forKey: "uvMultiplier")
        
        // Get current time components for UV interpolation
        let now = Date()
        let calendar = Calendar.current
        let hour = calendar.component(.hour, from: now)
        let minute = calendar.component(.minute, from: now)
        
        // Open-Meteo API - completely free, no API key needed!
        // Include elevation for more accurate UV calculations
        // Get 2 days of data (today and tomorrow) to reduce bandwidth
        let urlString = "https://api.open-meteo.com/v1/forecast?latitude=\(latitude)&longitude=\(longitude)&elevation=\(altitude)&daily=uv_index_max,uv_index_clear_sky_max,sunrise,sunset&hourly=uv_index,cloud_cover&timezone=auto&forecast_days=2"
        
        guard let url = URL(string: urlString) else {
            lastError = "Invalid URL"
            isLoading = false
            return
        }
        
        URLSession.shared.dataTaskPublisher(for: url)
            .map(\.data)
            .decode(type: OpenMeteoResponse.self, decoder: JSONDecoder())
            .receive(on: DispatchQueue.main)
            .sink(
                receiveCompletion: { [weak self] completion in
                    self?.isLoading = false
                    if case .failure(let error) = completion {
                        self?.lastError = error.localizedDescription
                        #if DEBUG
                        Self.logger.error("Fetch UV failed: \(error.localizedDescription, privacy: .public)")
                        self?.signposter.endInterval("FetchUV", fetchInterval)
                        #endif
                        // Try to load cached data in offline mode
                        self?.loadCachedData(for: location)
                    }
                },
                receiveValue: { [weak self] response in
                    guard let self = self else { return }
                    
                    
                    // Altitude already updated at the start of fetchUVData
                    
                    // Get today's data (first item in arrays)
                    if let todayMaxUV = response.daily.uvIndexMax.first {
                        self.maxUV = todayMaxUV
                    }
                    
                    // Get tomorrow's max UV
                    if response.daily.uvIndexMax.count > 1 {
                        self.tomorrowMaxUV = response.daily.uvIndexMax[1]
                    }
                    
                    // Parse sunrise and sunset times for today and tomorrow
                    let formatter = DateFormatter()
                    formatter.dateFormat = "yyyy-MM-dd'T'HH:mm"
                    formatter.timeZone = TimeZone.current
                    
                    // Today's times
                    if response.daily.sunrise.count > 0,
                       response.daily.sunset.count > 0 {
                        self.todaySunrise = formatter.date(from: response.daily.sunrise[0])
                        self.todaySunset = formatter.date(from: response.daily.sunset[0])
                        
                        // Tomorrow's times
                        if response.daily.sunrise.count > 1,
                           response.daily.sunset.count > 1 {
                            self.tomorrowSunrise = formatter.date(from: response.daily.sunrise[1])
                            self.tomorrowSunset = formatter.date(from: response.daily.sunset[1])
                            
                        }
                        
                        // Schedule notifications
                        self.scheduleSunNotifications()
                    }
                    
                    // Always fetch moon phase on first load, then update every 6 hours
                    if self.lastMoonPhaseUpdate == nil || 
                       Date().timeIntervalSince(self.lastMoonPhaseUpdate!) > 21600 {
                        self.fetchMoonPhase(for: location)
                    } else {
                        // Ensure moon phase is shared with widget
                        if !self.currentMoonPhaseName.isEmpty {
                            UserDefaults(suiteName: "group.sunday.widget")?.set(self.currentMoonPhaseName, forKey: "moonPhaseName")
                            WidgetCenter.shared.reloadAllTimelines()
                        } else {
                            self.fetchMoonPhase(for: location)
                        }
                    }
                    
                    // Get current hour's UV index with interpolation
                    if let hourlyUV = response.hourly?.uvIndex,
                       hour < hourlyUV.count {
                        let currentHourUV = hourlyUV[hour]
                        
                        // Calculate interpolation factor (0.0 to 1.0 based on minutes)
                        let interpolationFactor = Double(minute) / 60.0
                        
                        // Get next hour's UV for interpolation
                        var interpolatedUV = currentHourUV
                        if hour + 1 < hourlyUV.count {
                            let nextHourUV = hourlyUV[hour + 1]
                            // Linear interpolation between current and next hour
                            interpolatedUV = currentHourUV + (nextHourUV - currentHourUV) * interpolationFactor
                        }
                        
                        self.currentUV = interpolatedUV
                        self.apiUV = interpolatedUV
                    } else {
                        // Fallback: estimate current UV based on max and time of day
                        let estimated = self.estimateCurrentUV(maxUV: self.maxUV, hour: hour)
                        self.currentUV = estimated
                        self.apiUV = estimated
                    }

                    // Get current hour's cloud cover
                    if let cloudCover = response.hourly?.cloudCover,
                       hour < cloudCover.count {
                        self.apiCloudCover = cloudCover[hour]
                        self.currentCloudCover = cloudCover[hour]
                        // Share with widget
                        sharedDefaults?.set(self.currentCloudCover, forKey: "currentCloudCover")
                    }

                    // Clear any user override now that we have fresh API data
                    self.cloudCoverOverride = nil
                    
                    // Calculate safe exposure times
                    self.calculateSafeExposureTimes()
                    
                    // Check for vitamin D winter conditions
                    self.checkVitaminDWinter()
                    
                    // Clear offline mode on successful fetch
                    if self.isOfflineMode {
                        self.isOfflineMode = false
                    }
                    
                    // Trigger widget update
                    WidgetCenter.shared.reloadAllTimelines()
                    
                    // Cache the data for offline use
                    self.cacheUVData(response: response, location: location)
                    self.hasNoData = false
                    self.lastSuccessfulUpdate = Date()
                    #if DEBUG
                    Self.logger.debug("Fetch UV success maxUV=\(self.maxUV, privacy: .public) currentUV=\(self.currentUV, privacy: .public)")
                    self.signposter.endInterval("FetchUV", fetchInterval)
                    #endif
                }
            )
            .store(in: &cancellables)
    }
    
    private func estimateCurrentUV(maxUV: Double, hour: Int) -> Double {
        // UV follows a bell curve peaking around 1 PM
        let peakHour = 13.0
        let dayStart = 6.0
        let dayEnd = 19.0
        
        if Double(hour) < dayStart || Double(hour) > dayEnd {
            return 0.0
        }
        
        let hourFactor = 1.0 - (abs(Double(hour) - peakHour) / peakHour)
        return max(0, maxUV * hourFactor * 0.9)
    }
    
    private func calculateSafeExposureTimes() {
        // MED (Minimal Erythema Dose) times at UV index 1
        // Real-world values (not laboratory conditions)
        // These reflect actual outdoor exposure with natural cooling, movement, and typical base adaptation
        let medTimesAtUV1: [Int: Double] = [
            1: 150.0,  // Type I: Very fair skin (burns in ~30 min at UV 5)
            2: 250.0,  // Type II: Fair skin (burns in ~45-50 min at UV 5)
            3: 425.0,  // Type III: Light skin (burns in ~75-85 min at UV 5)
            4: 600.0,  // Type IV: Medium skin (burns in ~100-120 min at UV 5)
            5: 850.0,  // Type V: Dark skin (burns in ~150-180 min at UV 5)
            6: 1100.0  // Type VI: Very dark skin (rarely burns)
        ]
        
        let uvToUse = max(currentUV, 0.1)
        
        // Calculate burn time (full MED)
        burnTimeMinutes = [:]
        
        for (skinType, medTime) in medTimesAtUV1 {
            let fullMED = medTime / uvToUse
            burnTimeMinutes[skinType] = max(1, Int(fullMED))
        }
    }
    
    private func scheduleSunNotifications() {
        // Check if we already scheduled for today
        let calendar = Calendar.current
        let lastScheduledKey = "lastNotificationScheduledDate"
        let lastScheduledDate = UserDefaults.standard.object(forKey: lastScheduledKey) as? Date
        let isToday = lastScheduledDate.map { calendar.isDateInToday($0) } ?? false
        
        guard !isToday else { return }
        
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound]) { granted, _ in
            guard granted else { return }
            
            // Remove old notifications
            UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["sunrise", "sunset", "safeTimeReached", "solarNoon"])
            
            DispatchQueue.main.async {
                self.scheduleNotification(
                    at: self.todaySunrise,
                    title: "☀️ The sun is up!",
                    body: "Today's max UV index: \(Int(self.maxUV)). Start tracking your vitamin D!",
                    identifier: "sunrise"
                )
                
                self.scheduleNotification(
                    at: self.todaySunset,
                    title: "🌅 The sun is setting.",
                    body: "Check your vitamin D progress in Sun Day.",
                    identifier: "sunset"
                )
                
                // Schedule solar noon notification (30 minutes before)
                if let sunrise = self.todaySunrise, let sunset = self.todaySunset {
                    // Calculate solar noon as midpoint between sunrise and sunset
                    let sunriseTime = sunrise.timeIntervalSince1970
                    let sunsetTime = sunset.timeIntervalSince1970
                    let solarNoonTime = (sunriseTime + sunsetTime) / 2.0
                    let solarNoon = Date(timeIntervalSince1970: solarNoonTime)
                    
                    // Schedule notification 30 minutes before solar noon
                    let notificationTime = solarNoon.addingTimeInterval(-1800) // 30 minutes = 1800 seconds
                    
                    self.scheduleNotification(
                        at: notificationTime,
                        title: "☀️ Solar noon approaching!",
                        body: "Peak UV in 30 minutes (UV \(Int(self.maxUV))). Perfect time for vitamin D!",
                        identifier: "solarNoon"
                    )
                }
                
                UserDefaults.standard.set(Date(), forKey: lastScheduledKey)
                self.notificationScheduled = true
            }
        }
    }
    
    private func scheduleNotification(at date: Date?, title: String, body: String, identifier: String) {
        guard let date = date else { return }
        
        // Only schedule if the date is in the future
        if date <= Date() {
            return
        }
        
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        
        // Use time interval trigger for more reliable delivery
        let timeInterval = date.timeIntervalSinceNow
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: timeInterval, repeats: false)
        
        let request = UNNotificationRequest(identifier: identifier, content: content, trigger: trigger)
        
        UNUserNotificationCenter.current().add(request)
    }
    
    // Removed scheduleSafeTimeNotification and cancelSafeTimeNotification
    // Now using real-time MED tracking in VitaminDCalculator
    
    private func fetchMoonPhase(for location: CLLocation) {
        // Use Farmsense API - completely free, no API key needed!
        // API expects unix timestamp (uses first 10 digits)
        let timestamp = Int(Date().timeIntervalSince1970)
        let urlString = "http://api.farmsense.net/v1/moonphases/?d=\(timestamp)"
        
        guard let url = URL(string: urlString) else { 
            return 
        }
        
        URLSession.shared.dataTask(with: url) { [weak self] data, response, error in
            guard let self = self else { return }
            
            if error != nil { 
                // Set a default moon phase on error
                DispatchQueue.main.async {
                    self.currentMoonPhaseName = "Waxing Crescent"
                    UserDefaults(suiteName: "group.sunday.widget")?.set("Waxing Crescent", forKey: "moonPhaseName")
                    WidgetCenter.shared.reloadAllTimelines()
                }
                return 
            }
            
            guard let data = data else { 
                return 
            }
            
            do {
                if let json = try JSONSerialization.jsonObject(with: data) as? [[String: Any]],
                   let moonData = json.first {
                    
                    // Get illumination percentage (comes as 0-1 decimal)
                    let illuminationValue = moonData["Illumination"] as? Double ?? 0.0
                    
                    DispatchQueue.main.async {
                        // Simply use illumination value directly (0-1 scale)
                        self.currentMoonPhase = illuminationValue
                        
                        // Store phase name
                        if let phaseName = moonData["Phase"] as? String {
                            self.currentMoonPhaseName = phaseName
                            // Share with widget
                            UserDefaults(suiteName: "group.sunday.widget")?.set(phaseName, forKey: "moonPhaseName")
                            // Trigger widget update
                            WidgetCenter.shared.reloadAllTimelines()
                        }
                        
                        // Update last fetch time
                        self.lastMoonPhaseUpdate = Date()
                        
                    }
                }
            } catch {
                // Set a default moon phase on error
                DispatchQueue.main.async {
                    self.currentMoonPhaseName = "Waxing Crescent"
                    UserDefaults(suiteName: "group.sunday.widget")?.set("Waxing Crescent", forKey: "moonPhaseName")
                    WidgetCenter.shared.reloadAllTimelines()
                }
            }
        }.resume()
    }
    
    private func checkVitaminDWinter() {
        let calendar = Calendar.current
        let month = calendar.component(.month, from: Date())
        
        // Check for vitamin D winter conditions
        // Above 35° latitude: limited/no vitamin D synthesis in winter months
        if currentLatitude > 35 {
            switch month {
            case 11, 12, 1, 2:  // Nov-Feb
                isVitaminDWinter = true
            case 3, 10:  // Mar, Oct - marginal
                isVitaminDWinter = maxUV < 3.0
            default:
                isVitaminDWinter = false
            }
        } else {
            // Below 35° latitude - check if max UV is consistently below 3
            isVitaminDWinter = maxUV < 3.0
        }
    }
    
    private func cacheUVData(response: OpenMeteoResponse, location: CLLocation) {
        guard let modelContext = modelContext else { return }
        
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.timeZone = TimeZone.current
        
        // Cache data for each day
        for (index, dateString) in response.daily.time.enumerated() {
            guard let date = formatter.date(from: dateString),
                  index < response.daily.uvIndexMax.count else { continue }
            
            // Extract hourly data for this day
            let startHour = index * 24
            let endHour = min((index + 1) * 24, response.hourly?.uvIndex.count ?? 0)
            
            let hourlyUV = response.hourly?.uvIndex[startHour..<endHour].map { $0 } ?? []
            let hourlyCloudCover = response.hourly?.cloudCover?[startHour..<endHour].map { $0 } ?? []
            
            // Parse sunrise/sunset
            let sunriseString = response.daily.sunrise[safe: index] ?? ""
            let sunsetString = response.daily.sunset[safe: index] ?? ""
            
            let sunriseFormatter = DateFormatter()
            sunriseFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm"
            sunriseFormatter.timeZone = TimeZone.current
            let sunrise = sunriseFormatter.date(from: sunriseString) ?? date
            let sunset = sunriseFormatter.date(from: sunsetString) ?? date
            
            // Create or update cached data
            let cachedData = CachedUVData(
                latitude: location.coordinate.latitude,
                longitude: location.coordinate.longitude,
                date: date,
                hourlyUV: Array(hourlyUV),
                hourlyCloudCover: Array(hourlyCloudCover),
                maxUV: response.daily.uvIndexMax[index],
                sunrise: sunrise,
                sunset: sunset
            )
            
            modelContext.insert(cachedData)
        }
        
        // Save context
        do {
            try modelContext.save()
            // Clean up old cached data
            cleanupOldCachedData()
        } catch {
            // Failed to cache UV data
        }
    }
    
    private func loadCachedData(for location: CLLocation) {
        
        // Always ensure moon phase is available
        if currentMoonPhaseName.isEmpty {
            fetchMoonPhase(for: location)
        }
        
        guard let modelContext = modelContext else {
            isOfflineMode = true
            hasNoData = true
            return
        }
        
        // Always update altitude from GPS since it still works offline
        let validAltitude = location.altitude >= 0 ? location.altitude : 0
        currentAltitude = validAltitude
        let _ = validAltitude / 1000.0
        // In offline mode, also avoid scaling cached UV by altitude to prevent double counting.
        uvMultiplier = 1.0
        #if DEBUG
        Self.logger.debug("Offline with altitude: \(validAltitude, privacy: .public)m; using cached UV without extra scaling")
        #endif
        
        // Share with widget
        let sharedDefaults = UserDefaults(suiteName: "group.sunday.widget")
        sharedDefaults?.set(validAltitude, forKey: "currentAltitude")
        sharedDefaults?.set(uvMultiplier, forKey: "uvMultiplier")
        
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let tomorrow = calendar.date(byAdding: .day, value: 1, to: today)!
        
        // Store values in local constants for use in predicate
        let targetLatitude = location.coordinate.latitude
        let targetLongitude = location.coordinate.longitude
        let startDate = today
        let endDate = tomorrow
        
        // Fetch cached data for today and tomorrow
        // Use approximate location matching (within ~1km)
        let latTolerance = 0.01  // ~1.1km
        let lonTolerance = 0.01
        let minLat = targetLatitude - latTolerance
        let maxLat = targetLatitude + latTolerance
        let minLon = targetLongitude - lonTolerance
        let maxLon = targetLongitude + lonTolerance
        
        let descriptor = FetchDescriptor<CachedUVData>(
            predicate: #Predicate<CachedUVData> { data in
                data.latitude >= minLat &&
                data.latitude <= maxLat &&
                data.longitude >= minLon &&
                data.longitude <= maxLon &&
                data.date >= startDate &&
                data.date <= endDate  // Include tomorrow
            },
            sortBy: [SortDescriptor(\.date)]
        )
        
        do {
            let cachedData = try modelContext.fetch(descriptor)
            
            if let todayData = cachedData.first(where: { calendar.isDateInToday($0.date) }) {
                // Use cached data
                isOfflineMode = true
                
                // Save location for potential network restoration
                lastRetryLocation = location
                
                // Set current UV based on cached hourly data
                let hour = calendar.component(.hour, from: Date())
                if hour < todayData.hourlyUV.count {
                    currentUV = todayData.hourlyUV[hour]
                    apiUV = currentUV
                    if hour < todayData.hourlyCloudCover.count {
                        currentCloudCover = todayData.hourlyCloudCover[hour]
                        apiCloudCover = currentCloudCover
                        // Share with widget
                        sharedDefaults?.set(currentCloudCover, forKey: "currentCloudCover")
                    }
                }
                cloudCoverOverride = nil
                
                maxUV = todayData.maxUV
                todaySunrise = todayData.sunrise
                todaySunset = todayData.sunset
                
                // Get tomorrow's data if available
                if let tomorrowData = cachedData.first(where: { calendar.isDateInTomorrow($0.date) }) {
                    tomorrowMaxUV = tomorrowData.maxUV
                    tomorrowSunrise = tomorrowData.sunrise
                    tomorrowSunset = tomorrowData.sunset
                }
                
                calculateSafeExposureTimes()
                checkVitaminDWinter()
                
                // Trigger widget update
                WidgetCenter.shared.reloadAllTimelines()
            } else {
                isOfflineMode = true
                hasNoData = true
                // Save location for potential retry
                lastRetryLocation = location
            }
        } catch {
            isOfflineMode = true
            hasNoData = true
            // Save location for potential retry
            lastRetryLocation = location
        }
    }
    
    private func cleanupOldCachedData() {
        guard let modelContext = modelContext else { return }
        
        // Delete cached data older than 7 days
        let calendar = Calendar.current
        let cutoffDate = calendar.date(byAdding: .day, value: -7, to: Date()) ?? Date()
        
        let descriptor = FetchDescriptor<CachedUVData>(
            predicate: #Predicate<CachedUVData> { data in
                data.date < cutoffDate
            }
        )
        
        do {
            let oldData = try modelContext.fetch(descriptor)
            for data in oldData {
                modelContext.delete(data)
            }
            
            if !oldData.isEmpty {
                try modelContext.save()
            }
        } catch {
            // Failed to clean up old data
        }
    }

    // MARK: - Historical UV for today (from cache)
    func historicalUVPoints(from start: Date, to end: Date, near location: CLLocation?) -> [(Date, Double)] {
        guard let modelContext = modelContext else { return [] }
        let calendar = Calendar.current
        let dayStart = calendar.startOfDay(for: start)
        let dayEnd = calendar.date(byAdding: .day, value: 1, to: dayStart) ?? start

        // Resolve approximate coordinates for predicate
        let targetLatitude = location?.coordinate.latitude ?? currentLatitude
        // We don't store current longitude; prefer caller's location
        let targetLongitude = location?.coordinate.longitude ?? 0.0

        let latTolerance = 0.01
        let lonTolerance = 0.01
        let minLat = targetLatitude - latTolerance
        let maxLat = targetLatitude + latTolerance
        let minLon = targetLongitude - lonTolerance
        let maxLon = targetLongitude + lonTolerance

        let descriptor = FetchDescriptor<CachedUVData>(
            predicate: #Predicate<CachedUVData> { data in
                data.latitude >= minLat &&
                data.latitude <= maxLat &&
                data.longitude >= minLon &&
                data.longitude <= maxLon &&
                data.date >= dayStart &&
                data.date < dayEnd
            },
            sortBy: [SortDescriptor(\.date, order: .forward)]
        )

        do {
            let results = try modelContext.fetch(descriptor)
            guard let today = results.first else { return [] }

            // Build hourly timeline for the day
            var points: [(Date, Double)] = []
            let count = min(24, today.hourlyUV.count)
            for h in 0..<count {
                if let t = calendar.date(byAdding: .hour, value: h, to: dayStart) {
                    let uv = today.hourlyUV[h]
                    points.append((t, uv))
                }
            }

            guard !points.isEmpty else { return [] }

            // Helper to sample UV at arbitrary time using linear interpolation
            func sample(at date: Date) -> Double {
                let comp = calendar.dateComponents([.hour, .minute], from: date)
                let h = max(0, min((comp.hour ?? 0), count - 1))
                let baseUV = points[h].1
                if h + 1 < points.count {
                    let nextUV = points[h + 1].1
                    let frac = Double(comp.minute ?? 0) / 60.0
                    return baseUV + (nextUV - baseUV) * frac
                } else {
                    return baseUV
                }
            }

            // Assemble subrange including boundaries
            var window: [(Date, Double)] = []
            let startUV = sample(at: start)
            let endUV = sample(at: end)
            window.append((start, startUV))
            for (t, uv) in points where t > start && t < end {
                window.append((t, uv))
            }
            window.append((end, endUV))
            return window.sorted(by: { $0.0 < $1.0 })
        } catch {
            return []
        }
    }
}
