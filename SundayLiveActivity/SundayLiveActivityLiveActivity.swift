//
//  SundayLiveActivityLiveActivity.swift
//  SundayLiveActivity
//
//  Created by Shashi on 18/07/25.
//

import ActivityKit
import WidgetKit
import SwiftUI

struct SundayLiveActivityAttributes: ActivityAttributes {
    public struct ContentState: Codable, Hashable {
        // Dynamic stateful properties about your activity go here!
        var emoji: String
    }

    // Fixed non-changing properties about your activity go here!
    var name: String
}

struct SundayLiveActivityLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: SundayLiveActivityAttributes.self) { context in
            // Lock screen/banner UI goes here
            VStack {
                Text("Hello \(context.state.emoji)")
            }
            .activityBackgroundTint(Color.cyan)
            .activitySystemActionForegroundColor(Color.black)

        } dynamicIsland: { context in
            DynamicIsland {
                // Expanded UI goes here.  Compose the expanded UI through
                // various regions, like leading/trailing/center/bottom
                DynamicIslandExpandedRegion(.leading) {
                    Text("Leading")
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text("Trailing")
                }
                DynamicIslandExpandedRegion(.bottom) {
                    Text("Bottom \(context.state.emoji)")
                    // more content
                }
            } compactLeading: {
                Text("L")
            } compactTrailing: {
                Text("T \(context.state.emoji)")
            } minimal: {
                Text(context.state.emoji)
            }
            .widgetURL(URL(string: "http://www.apple.com"))
            .keylineTint(Color.red)
        }
    }
}

extension SundayLiveActivityAttributes {
    fileprivate static var preview: SundayLiveActivityAttributes {
        SundayLiveActivityAttributes(name: "World")
    }
}

extension SundayLiveActivityAttributes.ContentState {
    fileprivate static var smiley: SundayLiveActivityAttributes.ContentState {
        SundayLiveActivityAttributes.ContentState(emoji: "😀")
     }
     
     fileprivate static var starEyes: SundayLiveActivityAttributes.ContentState {
         SundayLiveActivityAttributes.ContentState(emoji: "🤩")
     }
}

#Preview("Notification", as: .content, using: SundayLiveActivityAttributes.preview) {
   SundayLiveActivityLiveActivity()
} contentStates: {
    SundayLiveActivityAttributes.ContentState.smiley
    SundayLiveActivityAttributes.ContentState.starEyes
}
