import Foundation

public struct ExploreFilters: Equatable, Sendable {
    public var keyword: String = ""
    public var dateFilter: DateFilter = .anytime
    public var onlyFree: Bool = false
    public var ageFilter: AgeFilter? = nil
    public var activeCategory: String? = nil

    public enum DateFilter: String, CaseIterable, Sendable {
        case anytime = "Anytime"
        case today = "Today"
        case weekend = "This weekend"
        case week = "This week"
        case month = "This month"
    }

    public enum AgeFilter: String, CaseIterable, Equatable, Sendable {
        case zeroToOne = "0–1 yr"
        case oneToThree = "1–3 yrs"
        case twoToFour = "2–4 yrs"
        case fiveToEight = "5–8 yrs"
        case nineAndUp = "9+ yrs"

        public var min: Int {
            switch self {
            case .zeroToOne: return 0
            case .oneToThree: return 1
            case .twoToFour: return 2
            case .fiveToEight: return 5
            case .nineAndUp: return 9
            }
        }

        public var max: Int? {
            switch self {
            case .zeroToOne: return 1
            case .oneToThree: return 3
            case .twoToFour: return 4
            case .fiveToEight: return 8
            case .nineAndUp: return nil
            }
        }
    }

    public init(
        keyword: String = "",
        dateFilter: DateFilter = .anytime,
        onlyFree: Bool = false,
        ageFilter: AgeFilter? = nil,
        activeCategory: String? = nil
    ) {
        self.keyword = keyword
        self.dateFilter = dateFilter
        self.onlyFree = onlyFree
        self.ageFilter = ageFilter
        self.activeCategory = activeCategory
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
        if ageFilter != nil { n += 1 }
        if activeCategory != nil { n += 1 }
        return n
    }
}
