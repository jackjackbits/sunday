import ActivityKit
import Foundation

@MainActor
class LiveActivityManager {
    static let shared = LiveActivityManager()
    private var activity: Activity<SundayActivityAttributes>?

    func startActivity(startDate: Date) {
        guard ActivityAuthorizationInfo().areActivitiesEnabled else {
            return
        }

        let initialState = SundayActivityAttributes.ContentState(
            elapsedTime: 0,
        )

        let attributes = SundayActivityAttributes(startDate: startDate)

        let content = ActivityContent(state: initialState, staleDate: nil)

        do {
            activity = try Activity<SundayActivityAttributes>.request(
                attributes: attributes,
                content: content,
                pushType: nil
            )
        } catch {
            print("Failed to start activity: \(error.localizedDescription)")
        }
    }

    func updateActivity(elapsedTime: TimeInterval, isPaused: Bool) {
        let newState = SundayActivityAttributes.ContentState(
            elapsedTime: elapsedTime,
        )

        let updatedContent = ActivityContent(state: newState, staleDate: nil)

        Task {
            await activity?.update(updatedContent)
        }
    }

    func stopActivity() {
        let finalState = SundayActivityAttributes.ContentState(
            elapsedTime: 0,
        )

        let finalContent = ActivityContent(state: finalState, staleDate: nil)

        Task {
            await activity?.end(finalContent, dismissalPolicy: .immediate)
            activity = nil
        }
    }
}
