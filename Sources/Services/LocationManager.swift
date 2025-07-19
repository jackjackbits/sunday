import Foundation
import CoreLocation
import WidgetKit
import Combine

class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    @Published var location: CLLocation?
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    @Published var isUpdatingLocation = false
    @Published var locationName: String = "" {
        didSet {
            // Share location name with widget
            sharedDefaults?.set(locationName, forKey: "locationName")
            // Trigger widget update
            WidgetCenter.shared.reloadAllTimelines()
        }
    }
    
    private let manager = CLLocationManager()
    private let geocoder = CLGeocoder()
    private let sharedDefaults = UserDefaults(suiteName: "group.jh.sunday.widget")
    
    // Geocoding cache
    private var geocodeCache: [String: String] = [:]
    private let geocodeCacheRadius: CLLocationDistance = 1000 // 1km radius for cache
    private var lastGeocodedLocation: CLLocation?
    
    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.distanceFilter = 500
        manager.pausesLocationUpdatesAutomatically = true
        manager.activityType = .fitness
        
        // Try to restore last known location from UserDefaults
        if let savedLat = UserDefaults.standard.object(forKey: "lastKnownLatitude") as? Double,
           let savedLon = UserDefaults.standard.object(forKey: "lastKnownLongitude") as? Double,
           let savedAlt = UserDefaults.standard.object(forKey: "lastKnownAltitude") as? Double {
            location = CLLocation(
                coordinate: CLLocationCoordinate2D(latitude: savedLat, longitude: savedLon),
                altitude: savedAlt,
                horizontalAccuracy: 100,
                verticalAccuracy: 50,
                timestamp: Date()
            )
            
            // Also restore location name
            if let savedName = UserDefaults.standard.string(forKey: "lastKnownLocationName") {
                locationName = savedName
            }
        }
    }
    
    func requestPermission() {
        manager.requestWhenInUseAuthorization()
    }
    
    // Keep this for general, low-power location updates
    func startUpdatingLocation() {
        manager.startUpdatingLocation()
    }
    
    func stopUpdatingLocation() {
        manager.stopUpdatingLocation()
    }
    
    // NEW: For use during a Live Activity session
    func startHighFrequencyUpdates() {
        manager.desiredAccuracy = kCLLocationAccuracyNearestTenMeters
        manager.distanceFilter = kCLDistanceFilterNone
        manager.allowsBackgroundLocationUpdates = true // Crucial for background execution
        manager.startUpdatingLocation()
    }
    
    // NEW: Revert to power-saving mode after a session
    func stopHighFrequencyUpdates() {
        manager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        manager.distanceFilter = 500
        manager.allowsBackgroundLocationUpdates = false
        // You can either stop updates completely or switch to a more efficient mode
        manager.startMonitoringSignificantLocationChanges()
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let newLocation = locations.last else { return }
        location = newLocation
        
        // Save location for offline use
        UserDefaults.standard.set(newLocation.coordinate.latitude, forKey: "lastKnownLatitude")
        UserDefaults.standard.set(newLocation.coordinate.longitude, forKey: "lastKnownLongitude")
        UserDefaults.standard.set(newLocation.altitude, forKey: "lastKnownAltitude")
        
        // Check if we need to reverse geocode (use cache if location is close enough)
        if let lastLocation = lastGeocodedLocation,
           newLocation.distance(from: lastLocation) < geocodeCacheRadius,
           let cachedName = getCachedLocationName(for: newLocation) {
            locationName = cachedName
        } else {
            geocoder.reverseGeocodeLocation(newLocation) { [weak self] placemarks, error in
                guard let self = self, let placemark = placemarks?.first else { return }
                
                DispatchQueue.main.async {
                    if let neighborhood = placemark.subLocality {
                        self.locationName = neighborhood
                    } else if let city = placemark.locality {
                        self.locationName = city
                    } else if let area = placemark.administrativeArea {
                        self.locationName = area
                    } else {
                        self.locationName = ""
                    }
                    
                    if !self.locationName.isEmpty {
                        self.cacheLocationName(self.locationName, for: newLocation)
                        self.lastGeocodedLocation = newLocation
                        UserDefaults.standard.set(self.locationName, forKey: "lastKnownLocationName")
                    }
                }
            }
        }
    }
    
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
        
        switch authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            startUpdatingLocation()
        default:
            stopUpdatingLocation()
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        // Silent error handling
    }
    
    // MARK: - Geocoding Cache
    
    private func getCachedLocationName(for location: CLLocation) -> String? {
        let key = geocodeCacheKey(for: location)
        return geocodeCache[key]
    }
    
    private func cacheLocationName(_ name: String, for location: CLLocation) {
        let key = geocodeCacheKey(for: location)
        geocodeCache[key] = name
        
        if geocodeCache.count > 50 {
            if let firstKey = geocodeCache.keys.first {
                geocodeCache.removeValue(forKey: firstKey)
            }
        }
    }
    
    private func geocodeCacheKey(for location: CLLocation) -> String {
        let lat = round(location.coordinate.latitude * 1000) / 1000
        let lon = round(location.coordinate.longitude * 1000) / 1000
        return "\(lat),\(lon)"
    }
}