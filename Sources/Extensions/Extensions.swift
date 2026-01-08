import Foundation
import SwiftUI

extension Array {
    subscript(safe index: Index) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}

extension Calendar {
    func isDateInTomorrow(_ date: Date) -> Bool {
        guard let tomorrow = Self.current.date(byAdding: DateComponents(day: 1), to: Date()) else {
            return false
        }
        return isDate(date, inSameDayAs: tomorrow)
    }
}
