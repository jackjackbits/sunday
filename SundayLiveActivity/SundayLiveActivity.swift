import ActivityKit
import SwiftUI
import WidgetKit

struct SundayLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: SundayActivityAttributes.self) { context in
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "sun.max.fill")
                        .foregroundColor(Color.yellow)
                        .font(.system(size: 28))

                    Spacer()

                    VStack(alignment: .trailing, spacing: 4) {
                        Text("In Sun")
                            .font(.title2)
                            .foregroundColor(Color.yellow)
                        
                        Text(elapsedTimeString(from: context.state.elapsedTime))
                            .font(.caption)
                            .monospacedDigit()
                            .bold()
                            .foregroundColor(Color.yellow)
                    }
                }
            }
            .padding()
            .background(Color.black)
            .activityBackgroundTint(.black)
            .activitySystemActionForegroundColor(Color.yellow)

        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded (long-press or music-style view)
                DynamicIslandExpandedRegion(.center) {
                    HStack {
                        // Sun icon
                        Image(systemName: "sun.max.fill")
                            .foregroundColor(Color.yellow)
                            .font(.system(size: 24))

                        VStack(alignment: .leading, spacing: 4) {
                            // Status
                            Text("In Sun")
                                .font(.headline)
                                .foregroundColor(Color.yellow)
                            
                            // Time
                            Text(elapsedTimeString(from: context.state.elapsedTime))
                                .font(.subheadline)
                                .monospacedDigit()
                                .foregroundColor(Color.yellow.opacity(0.3))
                        }
                    }
                    .padding(.horizontal)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color.black)
                }
            }

            // Compact leading (left pill)
            compactLeading: {
                Image(systemName: "sun.max.fill")
                    .foregroundColor(Color.yellow)
            }

            // Compact trailing (right pill)
            compactTrailing: {
                Text(elapsedShort(from: context.state.elapsedTime))
                    .foregroundColor(Color.yellow)
                    .monospacedDigit()
            }

            // Minimal (dot state)
            minimal: {
                Image(systemName: "sun.max.fill")
                    .foregroundColor(Color.yellow)
            }
        }

    }

    func elapsedTimeString(from interval: TimeInterval) -> String {
        let formatter = DateComponentsFormatter()
        formatter.unitsStyle = .positional
        formatter.allowedUnits = [.hour, .minute, .second]
        formatter.zeroFormattingBehavior = [.pad]
        return formatter.string(from: interval) ?? "00:00"
    }

    func elapsedShort(from interval: TimeInterval) -> String {
        let minutes = Int(interval) / 60
        return "\(minutes)m"
    }
}
