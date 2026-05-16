import Foundation

public struct ExploreFilters: Equatable, Sendable {
    public var keyword: String = ""
    public var dateFilter: DateFilter = .anytime
    public var onlyFree: Bool = false

    public enum DateFilter: String, CaseIterable, Sendable {
        case anytime = "Anytime"
        case today = "Today"
        case weekend = "This weekend"
        case week = "This week"
        case month = "This month"
    }

    public init(keyword: String = "", dateFilter: DateFilter = .anytime, onlyFree: Bool = false) {
        self.keyword = keyword
        self.dateFilter = dateFilter
        self.onlyFree = onlyFree
    }

    public var dateRange: (from: Date?, to: Date?) {
        let cal = Calendar.current
        let now = Date()
        let startOfToday = cal.startOfDay(for: now)
        switch dateFilter {
        case .anytime: return (nil, nil)
        case .today:
            return (startOfToday, cal.date(byAdding: .day, value: 1, to: startOfToday))
        case .weekend:
            let weekday = cal.component(.weekday, from: startOfToday)
            let daysUntilSat = (7 - weekday + 7) % 7
            let sat = cal.date(byAdding: .day, value: daysUntilSat, to: startOfToday)
            let monAfter = sat.flatMap { cal.date(byAdding: .day, value: 2, to: $0) }
            return (sat, monAfter)
        case .week:
            return (startOfToday, cal.date(byAdding: .day, value: 7, to: startOfToday))
        case .month:
            return (startOfToday, cal.date(byAdding: .month, value: 1, to: startOfToday))
        }
    }

    public var activeCount: Int {
        var n = 0
        if !keyword.isEmpty { n += 1 }
        if dateFilter != .anytime { n += 1 }
        if onlyFree { n += 1 }
        return n
    }
}
