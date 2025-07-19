import WidgetKit
import SwiftUI
import Intents
import Foundation

// Shared number formatter for widget
private let sharedNumberFormatter: NumberFormatter = {
    let formatter = NumberFormatter()
    formatter.numberStyle = .decimal
    formatter.maximumFractionDigits = 0
    return formatter
}()

struct Provider: IntentTimelineProvider {
    func placeholder(in context: Context) -> SimpleEntry {
        SimpleEntry(date: Date(), uvIndex: 5.0, todaysTotal: 2500, isTracking: false, vitaminDRate: 350, locationName: "Rome", moonPhaseName: "Full Moon", altitude: 100, uvMultiplier: 1.01, cloudCover: 20.0, configuration: ConfigurationIntent())
    }

    func getSnapshot(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (SimpleEntry) -> ()) {
        let entry = SimpleEntry(date: Date(), uvIndex: 5.0, todaysTotal: 2500, isTracking: false, vitaminDRate: 350, locationName: "Rome", moonPhaseName: "Full Moon", altitude: 100, uvMultiplier: 1.01, cloudCover: 20.0, configuration: configuration)
        completion(entry)
    }

    func getTimeline(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (Timeline<Entry>) -> ()) {
        var entries: [SimpleEntry] = []
        
        // Read from shared UserDefaults
        let sharedDefaults = UserDefaults(suiteName: "group.jh.sunday.widget")
        let uvIndex = sharedDefaults?.double(forKey: "currentUV") ?? 0.0
        let todaysTotal = sharedDefaults?.double(forKey: "todaysTotal") ?? 0.0
        let isTracking = sharedDefaults?.bool(forKey: "isTracking") ?? false
        let vitaminDRate = sharedDefaults?.double(forKey: "vitaminDRate") ?? 0.0
        let locationName = sharedDefaults?.string(forKey: "locationName") ?? ""
        var moonPhaseName = sharedDefaults?.string(forKey: "moonPhaseName") ?? ""
        let altitude = sharedDefaults?.double(forKey: "currentAltitude") ?? 0.0
        let uvMultiplier = sharedDefaults?.double(forKey: "uvMultiplier") ?? 1.0
        let cloudCover = sharedDefaults?.double(forKey: "currentCloudCover") ?? 0.0
        
        // Provide default if empty
        if moonPhaseName.isEmpty {
            moonPhaseName = "Waxing Gibbous"
        }

        // Generate timeline entries at key times when gradient changes
        let currentDate = Date()
        let calendar = Calendar.current
        
        // Create entries at gradient transition times
        var entryDates: [Date] = [currentDate]
        
        // Add entries for the next gradient transitions
        let gradientTransitionHours = [5.0, 6.0, 6.5, 7.0, 8.0, 10.0, 16.0, 17.0, 18.5, 19.5, 20.5, 22.0]
        
        for transitionHour in gradientTransitionHours {
            let transitionComponents = DateComponents(hour: Int(transitionHour), minute: Int((transitionHour.truncatingRemainder(dividingBy: 1)) * 60))
            if let transitionDate = calendar.nextDate(after: currentDate, matching: transitionComponents, matchingPolicy: .nextTime) {
                if transitionDate.timeIntervalSince(currentDate) < 24 * 60 * 60 { // Within 24 hours
                    entryDates.append(transitionDate)
                }
            }
        }
        
        // Sort and limit to reasonable number of entries
        entryDates.sort()
        entryDates = Array(entryDates.prefix(8))
        
        // Create entries
        for entryDate in entryDates {
            let entry = SimpleEntry(date: entryDate, uvIndex: uvIndex, todaysTotal: todaysTotal, isTracking: isTracking, vitaminDRate: vitaminDRate, locationName: locationName, moonPhaseName: moonPhaseName, altitude: altitude, uvMultiplier: uvMultiplier, cloudCover: cloudCover, configuration: configuration)
            entries.append(entry)
        }

        let timeline = Timeline(entries: entries, policy: .atEnd)
        completion(timeline)
    }
}

struct SimpleEntry: TimelineEntry {
    let date: Date
    let uvIndex: Double
    let todaysTotal: Double
    let isTracking: Bool
    let vitaminDRate: Double
    let locationName: String
    let moonPhaseName: String
    let altitude: Double
    let uvMultiplier: Double
    let cloudCover: Double
    let configuration: ConfigurationIntent
}

struct SundayWidgetEntryView : View {
    var entry: Provider.Entry
    @Environment(\.widgetFamily) var family

    var body: some View {
        switch family {
        case .systemSmall:
            SmallWidgetView(entry: entry)
        case .systemMedium:
            MediumWidgetView(entry: entry)
        default:
            Text("Not Supported")
        }
    }
}

struct SmallWidgetView: View {
    let entry: Provider.Entry
    
    var body: some View {
        VStack {
            // Main content
            VStack(spacing: 8) {
                // UV Index
                VStack(spacing: 2) {
                    Text("UV")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(.white.opacity(0.7))
                    Text(String(format: "%.1f", entry.uvIndex))
                        .font(.system(size: 28, weight: .bold))
                        .foregroundColor(.white)
                }
                
                // Today's total
                VStack(spacing: 2) {
                    Text("TODAY")
                        .font(.system(size: 10, weight: .medium))
                        .foregroundColor(.white.opacity(0.7))
                    Text(formatNumber(entry.todaysTotal))
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.white)
                    Text("IU")
                        .font(.system(size: 10))
                        .foregroundColor(.white.opacity(0.7))
                }
                
                // Tracking indicator
                if entry.isTracking {
                    Image(systemName: "sun.max.fill")
                        .font(.system(size: 14))
                        .foregroundColor(.yellow)
                }
            }
            
            Spacer()
            
            // Location at bottom left
            if !entry.locationName.isEmpty {
                HStack(spacing: 4) {
                    Image(systemName: "location.fill")
                        .font(.system(size: 9))
                        .foregroundColor(.white.opacity(0.6))
                    Text(entry.locationName)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(.white.opacity(0.8))
                        .lineLimit(1)
                    Spacer()
                }
            }
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .containerBackground(for: .widget) {
            LinearGradient(
                colors: gradientColors,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
    
    var gradientColors: [Color] {
        let hour = Calendar.current.component(.hour, from: Date())
        let minute = Calendar.current.component(.minute, from: Date())
        let timeProgress = Double(hour) + Double(minute) / 60.0
        
        if timeProgress < 5 || timeProgress > 22 {
            return [Color(hex: "0f1c3d"), Color(hex: "0a1228")]
        } else if timeProgress < 8 {
            return [Color(hex: "ee9b7a"), Color(hex: "fdb095")]
        } else if timeProgress < 10 {
            return [Color(hex: "fdb095"), Color(hex: "87ceeb")]
        } else if timeProgress < 17 {
            return [Color(hex: "4a90e2"), Color(hex: "7bb7e5")]
        } else if timeProgress < 19 {
            return [Color(hex: "87ceeb"), Color(hex: "fdb095")]
        } else if timeProgress < 20.5 {
            return [Color(hex: "ee9b7a"), Color(hex: "c44569")]
        } else {
            return [Color(hex: "c44569"), Color(hex: "6a4c93")]
        }
    }
    
    func formatNumber(_ value: Double) -> String {
        if value < 1000 {
            return "\(Int(value))"
        } else if value < 10000 {
            return sharedNumberFormatter.string(from: NSNumber(value: value)) ?? "\(Int(value))"
        } else {
            return String(format: "%.0fK", value / 1000)
        }
    }
}

struct MediumWidgetView: View {
    let entry: Provider.Entry
    
    var body: some View {
        ZStack {
            // Main content
            VStack(spacing: 0) {
                // Top row with UV info and button
                HStack(alignment: .top, spacing: 0) {
                    // Left side: UV Index and metrics
                    HStack(alignment: .top, spacing: 16) {
                        // UV Index section
                        VStack(alignment: .leading, spacing: 4) {
                            Text("UV INDEX")
                                .font(.system(size: 10, weight: .medium))
                                .foregroundColor(.white.opacity(0.7))
                            Text(String(format: "%.1f", entry.uvIndex))
                                .font(.system(size: 48, weight: .bold))
                                .foregroundColor(.white)
                        }
                        
                        // TODAY and RATE/POTENTIAL to the right
                        VStack(alignment: .leading, spacing: 12) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("TODAY")
                                    .font(.system(size: 10, weight: .medium))
                                    .foregroundColor(.white.opacity(0.7))
                                HStack(spacing: 2) {
                                    Text(formatNumber(entry.todaysTotal))
                                        .font(.system(size: 18, weight: .semibold))
                                        .foregroundColor(.white)
                                    Text("IU")
                                        .font(.system(size: 11))
                                        .foregroundColor(.white.opacity(0.8))
                                }
                            }
                            
                            if entry.uvIndex > 0 {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(entry.isTracking ? "RATE" : "POTENTIAL")
                                        .font(.system(size: 10, weight: .medium))
                                        .foregroundColor(.white.opacity(0.7))
                                    HStack(spacing: 2) {
                                        Text("\(Int(entry.vitaminDRate / 60))")
                                            .font(.system(size: 18, weight: .semibold))
                                            .foregroundColor(.white)
                                        Text("IU/min")
                                            .font(.system(size: 11))
                                            .foregroundColor(.white.opacity(0.8))
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer()
                    
                    // Right side: Button/Moon icon at top
                    if entry.uvIndex > 0 {
                        Link(destination: URL(string: "sunday://toggle")!) {
                            VStack(spacing: 4) {
                                Image(systemName: entry.isTracking ? "stop.circle.fill" : "sun.max.circle.fill")
                                    .font(.system(size: 44))
                                    .foregroundColor(.white)
                                Text(entry.isTracking ? "End" : "Begin")
                                    .font(.system(size: 12, weight: .medium))
                                    .foregroundColor(.white.opacity(0.8))
                            }
                        }
                    } else {
                        // Moon phase when UV is 0
                        VStack(spacing: 4) {
                            Image(systemName: moonPhaseIcon(from: entry.moonPhaseName))
                                .font(.system(size: 44))
                                .foregroundColor(.white.opacity(0.8))
                                .symbolRenderingMode(.hierarchical)
                            Text("Night")
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(.white.opacity(0.6))
                        }
                    }
                }
                
                Spacer()
                
                // Bottom info line: location, elevation, cloud
                HStack(spacing: 12) {
                    // Location with flexible space
                    if !entry.locationName.isEmpty {
                        HStack(spacing: 4) {
                            Image(systemName: "location.fill")
                                .font(.system(size: 9))
                                .foregroundColor(.white.opacity(0.6))
                            Text(entry.locationName)
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.white.opacity(0.8))
                                .lineLimit(1)
                                .truncationMode(.tail)
                                .layoutPriority(0)
                        }
                        .frame(minWidth: 0, maxWidth: .infinity, alignment: .leading)
                    }
                    
                    // Elevation with fixed space
                    if entry.altitude > 0 {
                        HStack(spacing: 4) {
                            Image(systemName: "arrow.up.to.line")
                                .font(.system(size: 10))
                                .foregroundColor(.white.opacity(0.6))
                            Text("\(Int(entry.altitude))m")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.white.opacity(0.6))
                            if entry.uvMultiplier > 1.0 {
                                Text("(+\(Int((entry.uvMultiplier - 1.0) * 100))%)")
                                    .font(.system(size: 10, weight: .medium))
                                    .foregroundColor(.white.opacity(0.5))
                            }
                        }
                        .layoutPriority(1)
                    }
                    
                    // Cloud cover with fixed space
                    if entry.cloudCover > 0 {
                        HStack(spacing: 4) {
                            Image(systemName: "cloud.fill")
                                .font(.system(size: 10))
                                .foregroundColor(.white.opacity(0.6))
                            Text("\(Int(entry.cloudCover))%")
                                .font(.system(size: 11, weight: .medium))
                                .foregroundColor(.white.opacity(0.6))
                        }
                        .layoutPriority(1)
                    }
                }
                .padding(.top, 6)
            }
            .padding(12)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .containerBackground(for: .widget) {
            LinearGradient(
                colors: gradientColors,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        }
    }
    
    var gradientColors: [Color] {
        let hour = Calendar.current.component(.hour, from: Date())
        let minute = Calendar.current.component(.minute, from: Date())
        let timeProgress = Double(hour) + Double(minute) / 60.0
        
        if timeProgress < 5 || timeProgress > 22 {
            return [Color(hex: "0f1c3d"), Color(hex: "0a1228")]
        } else if timeProgress < 8 {
            return [Color(hex: "ee9b7a"), Color(hex: "fdb095")]
        } else if timeProgress < 10 {
            return [Color(hex: "fdb095"), Color(hex: "87ceeb")]
        } else if timeProgress < 17 {
            return [Color(hex: "4a90e2"), Color(hex: "7bb7e5")]
        } else if timeProgress < 19 {
            return [Color(hex: "87ceeb"), Color(hex: "fdb095")]
        } else if timeProgress < 20.5 {
            return [Color(hex: "ee9b7a"), Color(hex: "c44569")]
        } else {
            return [Color(hex: "c44569"), Color(hex: "6a4c93")]
        }
    }
    
    func formatNumber(_ value: Double) -> String {
        if value < 1000 {
            return "\(Int(value))"
        } else if value < 10000 {
            return sharedNumberFormatter.string(from: NSNumber(value: value)) ?? "\(Int(value))"
        } else {
            return String(format: "%.0fK", value / 1000)
        }
    }
}

extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (255, 0, 0, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

func moonPhaseIcon(from phaseName: String) -> String {
    // Default icon if empty
    guard !phaseName.isEmpty else {
        return "moonphase.waxing.gibbous"
    }
    
    let phase = phaseName.lowercased()
    
    // Map phase names to SF Symbols
    // Note: Farmsense API has typo "Cresent" instead of "Crescent"
    let icon: String
    if phase.contains("new") {
        icon = "moonphase.new.moon"
    } else if phase.contains("waxing") && phase.contains("cres") {
        icon = "moonphase.waxing.crescent"
    } else if phase.contains("first quarter") {
        icon = "moonphase.first.quarter"
    } else if phase.contains("waxing") && phase.contains("gibbous") {
        icon = "moonphase.waxing.gibbous"
    } else if phase.contains("full") {
        icon = "moonphase.full.moon"
    } else if phase.contains("waning") && phase.contains("gibbous") {
        icon = "moonphase.waning.gibbous"
    } else if phase.contains("last quarter") || phase.contains("third quarter") {
        icon = "moonphase.last.quarter"
    } else if phase.contains("waning") && phase.contains("cres") {
        icon = "moonphase.waning.crescent"
    } else {
        // Fallback
        icon = "moonphase.waxing.gibbous"
    }
    
    return icon
}

@main
struct SundayWidgetBundle: WidgetBundle {
    init() {
    }
    
    var body: some Widget {
        SundayWidget()
        if #available(iOS 16.1, *) {
            LiveActivityWidget()
        }
    }
}

struct SundayWidget: Widget {
    let kind: String = "SundayWidget"

    var body: some WidgetConfiguration {
        IntentConfiguration(kind: kind, intent: ConfigurationIntent.self, provider: Provider()) { entry in
            SundayWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("Sun Day")
        .description("Track UV and vitamin D intake")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

struct SundayWidget_Previews: PreviewProvider {
    static var previews: some View {
        SundayWidgetEntryView(entry: SimpleEntry(date: Date(), uvIndex: 5.0, todaysTotal: 2500, isTracking: false, vitaminDRate: 350, locationName: "Rome", moonPhaseName: "Full Moon", altitude: 100, uvMultiplier: 1.01, cloudCover: 20.0, configuration: ConfigurationIntent()))
            .previewContext(WidgetPreviewContext(family: .systemSmall))
    }
}