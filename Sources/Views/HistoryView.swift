import SwiftUI
import Charts

// MARK: – HistoryView
//
// Bar chart of daily vitamin D synthesis with a half-life-weighted moving
// average line. The MA uses the pharmacokinetic half-life of circulating
// 25-hydroxyvitamin D (calcidiol) — approximately 20 days — so each past
// day's contribution decays as  weight = 0.966^offset  (where 0.966 ≈
// 2^(-1/20)). This mirrors how the body actually accumulates and loses the
// vitamin: if the line trends up you are consistently banking sun exposure;
// if it trends down you are falling behind. The MA is seeded with up to
// 3 × half-life (≈ 60 days) of extra history so the displayed window starts
// with an accurate value rather than a cold-start of zero.

struct HistoryView: View {
    @EnvironmentObject var healthManager: HealthManager
    @AppStorage("usesMCG") private var usesMCG: Bool = false
    @Environment(\.dismiss) var dismiss

    // MARK: Period
    enum Period: String, CaseIterable {
        case week = "W", month = "M", threeMonth = "3M"

        /// How many daily bars to display
        var displayDays: Int {
            switch self {
            case .week:       return 7
            case .month:      return 30
            case .threeMonth: return 90
            }
        }

        var avgLabel: String {
            switch self {
            case .week:       return "AVG / DAY  ·  7 DAYS"
            case .month:      return "AVG / DAY  ·  30 DAYS"
            case .threeMonth: return "AVG / DAY  ·  3 MONTHS"
            }
        }

        /// Desired number of x-axis labels
        var axisLabelCount: Int {
            switch self {
            case .week:       return 7
            case .month:      return 5
            case .threeMonth: return 4
            }
        }
    }

    // MARK: Data models
    struct Bar: Identifiable {
        let id = UUID()
        let date: Date
        let iu: Double
    }

    struct MAPoint: Identifiable {
        let id = UUID()
        let date: Date
        let iu: Double   // half-life-weighted moving average in IU
    }

    // MARK: State
    @State private var period: Period = .week
    @State private var bars: [Bar] = []
    @State private var maPoints: [MAPoint] = []
    @State private var isLoading = true

    // MARK: Helpers
    private var unitLabel: String { usesMCG ? "mcg" : "IU" }
    private func convert(_ iu: Double) -> Double { usesMCG ? iu / 40.0 : iu }

    private var average: Double {
        guard !bars.isEmpty else { return 0 }
        return convert(bars.reduce(0.0) { $0 + $1.iu } / Double(bars.count))
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

                ScrollView {
                    VStack(spacing: 20) {

                        // Period picker
                        Picker("Period", selection: $period) {
                            ForEach(Period.allCases, id: \.self) { p in
                                Text(p.rawValue).tag(p)
                            }
                        }
                        .pickerStyle(.segmented)
                        .padding(.horizontal, 20)
                        .padding(.top, 8)

                        // Average display
                        VStack(spacing: 4) {
                            Text(period.avgLabel)
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.white.opacity(0.6))
                                .tracking(1.2)
                            Text(formatValue(average))
                                .font(.system(size: 52, weight: .bold, design: .rounded))
                                .foregroundColor(.white)
                                .monospacedDigit()
                            Text(unitLabel)
                                .font(.system(size: 16))
                                .foregroundColor(.white.opacity(0.7))
                        }

                        // Chart
                        if isLoading {
                            ProgressView().tint(.white).frame(height: 240)
                        } else {
                            VStack(spacing: 8) {
                                Chart {
                                    // Daily bars
                                    ForEach(bars) { bar in
                                        BarMark(
                                            x: .value("Date", bar.date, unit: .day),
                                            y: .value(unitLabel, convert(bar.iu))
                                        )
                                        .foregroundStyle(.white.opacity(0.75))
                                        .cornerRadius(2)
                                    }

                                    // Half-life-weighted moving average line
                                    ForEach(maPoints) { pt in
                                        LineMark(
                                            x: .value("Date", pt.date, unit: .day),
                                            y: .value("Trend", convert(pt.iu))
                                        )
                                        .foregroundStyle(Color.yellow.opacity(0.9))
                                        .lineStyle(StrokeStyle(lineWidth: 2, lineCap: .round))
                                        .interpolationMethod(.catmullRom)
                                    }
                                }
                                .chartXAxis {
                                    AxisMarks(values: .automatic(desiredCount: period.axisLabelCount)) { _ in
                                        AxisValueLabel(format: xFormat, centered: true)
                                            .foregroundStyle(Color.white.opacity(0.7))
                                            .font(.system(size: 11))
                                    }
                                }
                                .chartYAxis {
                                    AxisMarks(position: .leading) { value in
                                        AxisValueLabel {
                                            if let v = value.as(Double.self) {
                                                Text(formatValue(v))
                                                    .font(.system(size: 10))
                                                    .foregroundStyle(Color.white.opacity(0.6))
                                            }
                                        }
                                        AxisGridLine()
                                            .foregroundStyle(Color.white.opacity(0.12))
                                    }
                                }
                                .chartPlotStyle { plot in
                                    plot.background(Color.black.opacity(0.15))
                                        .cornerRadius(12)
                                }
                                .frame(height: 240)
                                .padding(.horizontal, 20)

                                // Legend
                                HStack(spacing: 18) {
                                    HStack(spacing: 6) {
                                        RoundedRectangle(cornerRadius: 2)
                                            .fill(.white.opacity(0.75))
                                            .frame(width: 14, height: 10)
                                        Text("Daily synthesis")
                                    }
                                    HStack(spacing: 6) {
                                        Capsule()
                                            .fill(Color.yellow.opacity(0.9))
                                            .frame(width: 18, height: 2.5)
                                        Text("Body store trend")
                                    }
                                }
                                .font(.system(size: 11))
                                .foregroundColor(.white.opacity(0.65))
                            }
                        }

                        // MA explanation
                        if !isLoading {
                            Text("Trend line uses the 20-day half-life of 25(OH)D — rising means you're consistently building stores, falling means you need more sun.")
                                .font(.system(size: 11))
                                .foregroundColor(.white.opacity(0.5))
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 28)
                        }

                        Spacer(minLength: 20)
                    }
                }
            }
            .navigationTitle("Vitamin D History")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                        .foregroundColor(.white)
                }
            }
            .preferredColorScheme(.dark)
        }
        .onAppear { loadData() }
        .onChange(of: period) { loadData() }
    }

    // MARK: X-axis format
    private var xFormat: Date.FormatStyle {
        switch period {
        case .week:       return .dateTime.weekday(.abbreviated)
        case .month:      return .dateTime.month(.abbreviated).day()
        case .threeMonth: return .dateTime.month(.abbreviated)
        }
    }

    // MARK: Data loading
    private func loadData() {
        isLoading = true

        // Fetch enough extra history to warm up the MA before the display window.
        // 3 × half-life ≈ 60 days of seed data means the MA starts accurate.
        let halfLifeDays = 20
        let seedDays = halfLifeDays * 3
        let fetchDays = period.displayDays + seedDays

        healthManager.getVitaminDHistory(days: fetchDays) { dailyTotals in
            let calendar = Calendar.current
            let today = calendar.startOfDay(for: Date())

            // Build a complete ordered array for every fetched day (oldest → newest)
            let allBars: [Bar] = (0..<fetchDays).compactMap { offset -> Bar? in
                let daysAgo = fetchDays - 1 - offset
                guard let date = calendar.date(byAdding: .day, value: -daysAgo, to: today) else { return nil }
                return Bar(date: date, iu: dailyTotals[date] ?? 0)
            }

            // Displayed bars = the most-recent `displayDays` slice
            self.bars = Array(allBars.suffix(period.displayDays))

            // Compute MA across ALL fetched days, then take the same suffix
            let allMA = Self.halfLifeWMA(allBars: allBars, halfLifeDays: Double(halfLifeDays))
            self.maPoints = Array(allMA.suffix(period.displayDays))

            self.isLoading = false
        }
    }

    // MARK: Half-life weighted moving average
    //
    // For each day i the weighted average is:
    //   WMA(i) = Σ_{k=0}^{K} ( iu[i-k] × decay^k ) / Σ_{k=0}^{K} decay^k
    // where decay = 2^(−1/halfLife) ≈ 0.9659 for halfLife = 20 days.
    //
    // Rising = you are outpacing decay (building stores).
    // Falling = you are not getting enough sun to maintain levels.
    static func halfLifeWMA(allBars: [Bar], halfLifeDays: Double) -> [MAPoint] {
        let decay = pow(0.5, 1.0 / halfLifeDays)
        var points: [MAPoint] = []
        points.reserveCapacity(allBars.count)

        for i in allBars.indices {
            var weightedSum = 0.0
            var totalWeight  = 0.0
            let lookback = min(i + 1, Int(ceil(halfLifeDays * 3)))
            for offset in 0..<lookback {
                let w = pow(decay, Double(offset))
                weightedSum += allBars[i - offset].iu * w
                totalWeight  += w
            }
            let wma = totalWeight > 0 ? weightedSum / totalWeight : 0
            points.append(MAPoint(date: allBars[i].date, iu: wma))
        }
        return points
    }

    // MARK: Formatting
    private func formatValue(_ value: Double) -> String {
        if value == 0  { return "0" }
        if value < 1   { return String(format: "%.1f", value) }
        if value < 1000 { return "\(Int(value))" }
        let f = NumberFormatter()
        f.numberStyle = .decimal
        f.maximumFractionDigits = 0
        return f.string(from: NSNumber(value: value)) ?? "\(Int(value))"
    }
}
