import SwiftUI
import HealthKit
import SwiftData

struct SessionCompletionSheet: View {
    @Environment(\.dismiss) var dismiss
    @EnvironmentObject var vitaminDCalculator: VitaminDCalculator
    @EnvironmentObject var healthManager: HealthManager
    @Environment(\.modelContext) private var modelContext
    @AppStorage("usesMCG") private var usesMCG: Bool = false

    let sessionStartTime: Date
    let sessionAmount: Double
    let onSave: () -> Void
    let onCancel: () -> Void

    @State private var selectedStartTime: Date
    @State private var selectedEndTime: Date
    /// Live amount — updated 0.5 s after either time wheel stops moving
    @State private var currentAmount: Double
    /// Debounce handle for recalculation
    @State private var recalcTask: Task<Void, Never>?
    /// Duration at the moment the sheet opened — used as the denominator for scaling
    private let originalDuration: TimeInterval

    init(sessionStartTime: Date, sessionAmount: Double,
         onSave: @escaping () -> Void, onCancel: @escaping () -> Void) {
        self.sessionStartTime = sessionStartTime
        self.sessionAmount = sessionAmount
        self.onSave = onSave
        self.onCancel = onCancel
        let now = Date()
        self._selectedStartTime = State(initialValue: sessionStartTime)
        self._selectedEndTime   = State(initialValue: now)
        self._currentAmount     = State(initialValue: sessionAmount)
        self.originalDuration   = now.timeIntervalSince(sessionStartTime)
    }

    // MARK: Computed helpers

    private var sessionDuration: TimeInterval {
        max(0, selectedEndTime.timeIntervalSince(selectedStartTime))
    }

    private var formattedDuration: String {
        let minutes = Int(sessionDuration / 60)
        if minutes < 60 {
            return "\(minutes) min"
        } else {
            let hours = minutes / 60
            let rem = minutes % 60
            return rem == 0 ? "\(hours) hr" : "\(hours) hr \(rem) min"
        }
    }

    private var formattedAmount: String {
        let value = usesMCG ? currentAmount / 40.0 : currentAmount
        let unit  = usesMCG ? "mcg" : "IU"
        if value == 0  { return "0 \(unit)" }
        if value < 1   { return String(format: "%.1f \(unit)", value) }
        if value < 1000 { return "\(Int(value)) \(unit)" }
        if value < 100_000 {
            let f = NumberFormatter()
            f.numberStyle = .decimal
            f.maximumFractionDigits = 0
            return "\(f.string(from: NSNumber(value: value)) ?? "\(Int(value))") \(unit)"
        }
        return String(format: "%.0fK \(unit)", value / 1000)
    }

    // MARK: Body

    var body: some View {
        NavigationView {
            ZStack {
                LinearGradient(
                    colors: [Color(hex: "4a90e2"), Color(hex: "7bb7e5")],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()

                VStack(spacing: 0) {
                    VStack(spacing: 20) {
                        Image(systemName: "sun.max.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.yellow)
                            .symbolEffect(.pulse)
                            .padding(.top, 10)

                        VStack(spacing: 16) {
                            // Vitamin D amount — animates when recalculated
                            VStack(spacing: 4) {
                                Text("VITAMIN D SYNTHESIZED")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(.white.opacity(0.7))
                                    .tracking(1.2)
                                Text(formattedAmount)
                                    .font(.system(size: 36, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                                    .animation(.easeInOut(duration: 0.25), value: currentAmount)
                                    .contentTransition(.numericText())
                            }

                            // Duration
                            VStack(spacing: 4) {
                                Text("SESSION DURATION")
                                    .font(.system(size: 12, weight: .bold))
                                    .foregroundColor(.white.opacity(0.7))
                                    .tracking(1.2)
                                Text(formattedDuration)
                                    .font(.system(size: 26, weight: .semibold, design: .rounded))
                                    .foregroundColor(.white)
                                    .animation(.easeInOut(duration: 0.25), value: sessionDuration)
                                    .contentTransition(.numericText())
                            }
                        }
                        .padding(.horizontal, 30)

                        VStack(spacing: 16) {
                            // Editable start time
                            VStack(alignment: .leading, spacing: 8) {
                                Text("START TIME")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(.white.opacity(0.6))
                                    .tracking(1.2)
                                DatePicker("", selection: $selectedStartTime,
                                           in: ...selectedEndTime,
                                           displayedComponents: [.hourAndMinute])
                                    .datePickerStyle(.wheel)
                                    .labelsHidden()
                                    .colorScheme(.dark)
                                    .frame(height: 100)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding()
                            .background(Color.black.opacity(0.2))
                            .cornerRadius(12)

                            // Editable end time
                            VStack(alignment: .leading, spacing: 8) {
                                Text("END TIME")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(.white.opacity(0.6))
                                    .tracking(1.2)
                                DatePicker("", selection: $selectedEndTime,
                                           in: selectedStartTime...Date(),
                                           displayedComponents: [.hourAndMinute])
                                    .datePickerStyle(.wheel)
                                    .labelsHidden()
                                    .colorScheme(.dark)
                                    .frame(height: 100)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding()
                            .background(Color.black.opacity(0.2))
                            .cornerRadius(12)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 10)
                    }

                    Spacer()

                    VStack(spacing: 12) {
                        Button(action: { saveSession() }) {
                            HStack {
                                Image(systemName: "heart.fill")
                                    .font(.system(size: 18))
                                Text("Save to Health")
                                    .font(.system(size: 18, weight: .semibold))
                            }
                            .foregroundColor(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 16)
                            .background(Color.white.opacity(0.2))
                            .cornerRadius(15)
                        }

                        Button(action: { onCancel(); dismiss() }) {
                            Text("Continue Tracking")
                                .font(.system(size: 16, weight: .medium))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                        }

                        Button(action: { endWithoutSaving() }) {
                            Text("End and Don't Save")
                                .font(.system(size: 16, weight: .medium))
                                .foregroundColor(.white.opacity(0.7))
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 12)
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 20)
                }
            }
            .navigationTitle("Session Complete")
            .navigationBarTitleDisplayMode(.inline)
            .preferredColorScheme(.dark)
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .presentationBackground(.clear)
        // Recalculate vitamin D 0.5 s after either wheel stops spinning
        .onChange(of: selectedStartTime) { _, _ in scheduleRecalculation() }
        .onChange(of: selectedEndTime)   { _, _ in scheduleRecalculation() }
    }

    // MARK: Recalculation (debounced 0.5 s)
    //
    // The accumulated session amount scales linearly with duration — if the user
    // adds or removes time we apply the same ratio to the original amount.
    // This preserves the UV / skin / clothing weighting from the live session
    // without needing to re-run the full kinetics model.
    private func scheduleRecalculation() {
        recalcTask?.cancel()
        recalcTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 500_000_000)   // 0.5 s
            guard !Task.isCancelled, originalDuration > 0 else { return }
            let newDuration = max(0, selectedEndTime.timeIntervalSince(selectedStartTime))
            currentAmount = sessionAmount * (newDuration / originalDuration)
        }
    }

    // MARK: Helpers

    private func saveSession() {
        // Save the recalculated amount (currentAmount) to HealthKit, not the
        // original sessionAmount, so the Health record matches the adjusted times.
        healthManager.saveVitaminD(amount: currentAmount, date: selectedStartTime) { _ in
            vitaminDCalculator.refreshTodayTotals(forceWidget: true)
            let session = VitaminDSession(
                startTime: selectedStartTime,
                totalIU: currentAmount,
                averageUV: 0,
                peakUV: 0,
                clothingLevel: vitaminDCalculator.clothingLevel.rawValue,
                skinType: vitaminDCalculator.skinType.rawValue
            )
            session.endTime = selectedEndTime

            modelContext.insert(session)
            try? modelContext.save()

            onSave()
            dismiss()
        }
    }

    private func endWithoutSaving() {
        vitaminDCalculator.sessionVitaminD = 0.0
        vitaminDCalculator.toggleSunExposure(uvIndex: 0)
        dismiss()
    }
}
