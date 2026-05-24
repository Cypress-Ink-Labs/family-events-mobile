import Foundation
import Observation
import FECore
import FEData

@MainActor
@Observable
public final class CalendarViewModel: Refreshable {
    public private(set) var eventsByDay: [Date: [EventDTO]] = [:]
    public private(set) var isLoading = false
    public private(set) var errorMessage: String?
    public private(set) var lastFetchedAt: Date?
    public var selectedDate: Date
    public private(set) var displayedMonth: Date

    private let eventRepo: any EventRepository
    private let calendar: Calendar
    private let userID: UserID
    private var cityID: CityID?

    public init(
        eventRepo: any EventRepository,
        userID: UserID,
        cityID: CityID?,
        calendar: Calendar = .current,
        now: Date = Date()
    ) {
        self.eventRepo = eventRepo
        self.userID = userID
        self.cityID = cityID
        self.calendar = calendar
        self.selectedDate = calendar.startOfDay(for: now)
        self.displayedMonth = calendar.startOfMonth(for: now)
    }

    public func refresh() async {
        await loadMonth(displayedMonth, bypassCache: true)
    }

    public func loadIfNeeded() async {
        if eventsByDay.isEmpty || !CacheTTL.isFresh(lastFetchedAt: lastFetchedAt, ttl: CacheTTL.default) {
            await loadMonth(displayedMonth, bypassCache: false)
        }
    }

    public func updateCity(_ cityID: CityID?) async {
        guard cityID != self.cityID else { return }
        self.cityID = cityID
        await refresh()
    }

    public func moveMonth(by offset: Int) async {
        guard let nextMonth = calendar.date(byAdding: .month, value: offset, to: displayedMonth) else {
            return
        }
        displayedMonth = calendar.startOfMonth(for: nextMonth)
        let currentDay = calendar.component(.day, from: selectedDate)
        let daysInMonth = calendar.range(of: .day, in: .month, for: displayedMonth)?.count ?? 1
        let clampedDay = min(currentDay, daysInMonth)
        if let newSelected = calendar.date(bySetting: .day, value: clampedDay, of: displayedMonth) {
            selectedDate = calendar.startOfDay(for: newSelected)
        }
        await loadMonth(displayedMonth, bypassCache: true)
    }

    private func loadMonth(_ month: Date, bypassCache: Bool) async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        let monthStart = calendar.startOfMonth(for: month)
        guard let monthEnd = calendar.date(byAdding: DateComponents(month: 1, second: -1), to: monthStart) else {
            return
        }

        do {
            let fetched = try await eventRepo.fetchList(
                query: EventQuery(
                    cityID: cityID,
                    dateFrom: monthStart,
                    dateTo: monthEnd,
                    limit: 500,
                    offset: 0
                ),
                for: userID
            )
            eventsByDay = Dictionary(grouping: fetched) { event in
                calendar.startOfDay(for: event.startDatetime)
            }
            lastFetchedAt = Date()
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }

    public func events(on date: Date) -> [EventDTO] {
        eventsByDay[calendar.startOfDay(for: date)] ?? []
    }

    public func hasEvents(on date: Date) -> Bool {
        !events(on: date).isEmpty
    }
}

public extension Calendar {
    func startOfMonth(for date: Date) -> Date {
        let comps = dateComponents([.year, .month], from: date)
        return self.date(from: comps) ?? date
    }
}
