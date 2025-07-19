import Foundation
import ActivityKit

struct SundayActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        // Dynamic data that can be updated
        var sessionDuration: TimeInterval
        var currentUVIndex: Double
        var sessionVitaminD: Double
    }

    // Static data that is set when the activity is started
    var appName: String = "Sun Day"
}