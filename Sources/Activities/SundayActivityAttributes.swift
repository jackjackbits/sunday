import ActivityKit
import Foundation

struct SundayActivityAttributes: ActivityAttributes {
    
    public struct ContentState: Codable, Hashable {
        var elapsedTime: TimeInterval
    }

    var startDate: Date
}
