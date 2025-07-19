import ActivityKit
import WidgetKit
import SwiftUI

struct LiveActivityWidget: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: SundayActivityAttributes.self) { context in
            // Lock screen/banner UI
            VStack {
                Text("Sun Day Session")
                    .font(.headline)
                HStack {
                    VStack(alignment: .leading) {
                        Text("Duration")
                            .font(.caption)
                        Text(formatDuration(context.state.sessionDuration))
                            .font(.title2)
                    }
                    Spacer()
                    VStack(alignment: .center) {
                        Text("UV Index")
                            .font(.caption)
                        Text(String(format: "%.1f", context.state.currentUVIndex))
                            .font(.title2)
                    }
                    Spacer()
                    VStack(alignment: .trailing) {
                        Text("Vitamin D")
                            .font(.caption)
                        Text("\(Int(context.state.sessionVitaminD)) IU")
                            .font(.title2)
                    }
                }
            }
            .padding()
            .activityBackgroundTint(Color.cyan)
            .activitySystemActionForegroundColor(Color.black)

        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded UI
                DynamicIslandExpandedRegion(.leading) {
                    Text("Duration")
                    Text(formatDuration(context.state.sessionDuration))
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text("UV")
                    Text(String(format: "%.1f", context.state.currentUVIndex))
                }
                DynamicIslandExpandedRegion(.center) {
                    Text("\(Int(context.state.sessionVitaminD)) IU")
                }
                DynamicIslandExpandedRegion(.bottom) {
                   Text("Vitamin D Session")
                }
            } compactLeading: {
                Image(systemName: "sun.max.fill")
            } compactTrailing: {
                Text("\(Int(context.state.sessionVitaminD)) IU")
            } minimal: {
                Image(systemName: "sun.max.fill")
            }
            .widgetURL(URL(string: "http://www.apple.com"))
            .keylineTint(Color.cyan)
        }
    }

    func formatDuration(_ duration: TimeInterval) -> String {
        let minutes = Int(duration) / 60
        let seconds = Int(duration) % 60
        return String(format: "%02d:%02d", minutes, seconds)
    }
}