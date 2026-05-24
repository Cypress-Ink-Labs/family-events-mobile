import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FECalendar

@MainActor
final class CalendarViewModelTests: XCTestCase {
    private let calendar = Calendar(identifier: .gregorian)

    private func event(id: String, daysFromNow: Int, isFree: Bool = false) -> EventDTO {
        let start = calendar.date(byAdding: .day, value: daysFromNow, to: Date()) ?? Date()
        return EventDTO(
            id: EventID(id),
            title: "Event \(id)",
            description: nil,
            startDatetime: start,
            endDatetime: nil,
            timezone: "UTC",
            venueName: nil,
            address: nil,
            cityID: nil,
            latitude: nil,
            longitude: nil,
            ageMin: nil,
            ageMax: nil,
            price: isFree ? nil : 5,
            isFree: isFree,
            sourceURL: nil,
            sourceName: nil,
            sourceID: nil,
            images: [],
            status: "published",
            aiConfidence: nil,
            aiTagProvider: nil,
            isFeatured: false,
            viewCount: 0,
            createdAt: Date(timeIntervalSince1970: 0),
            updatedAt: Date(timeIntervalSince1970: 0),
            tags: [],
            avgRating: 0,
            ratingCount: 0,
            isFavorited: false
        )
    }

    func test_refresh_groupsEventsByDay() async {
        let repo = FakeEventRepository()
        repo.fetchListResult = .success([
            event(id: "e1", daysFromNow: 0),
            event(id: "e2", daysFromNow: 0),
            event(id: "e3", daysFromNow: 3),
        ])
        let vm = CalendarViewModel(eventRepo: repo, userID: UserID("u1"), cityID: nil, calendar: calendar)
        await vm.refresh()
        XCTAssertEqual(vm.eventsByDay.values.flatMap { $0 }.count, 3)
        let today = calendar.startOfDay(for: Date())
        XCTAssertEqual(vm.events(on: today).count, 2)
    }

    func test_hasEvents_isTrueOnlyWhenGrouped() async {
        let repo = FakeEventRepository()
        repo.fetchListResult = .success([event(id: "e1", daysFromNow: 0)])
        let vm = CalendarViewModel(eventRepo: repo, userID: UserID("u1"), cityID: nil, calendar: calendar)
        await vm.refresh()
        let today = calendar.startOfDay(for: Date())
        let tomorrow = calendar.date(byAdding: .day, value: 1, to: today)!
        XCTAssertTrue(vm.hasEvents(on: today))
        XCTAssertFalse(vm.hasEvents(on: tomorrow))
    }

    func test_loadIfNeeded_respectsCacheTTL() async {
        let repo = FakeEventRepository()
        repo.fetchListResult = .success([event(id: "e1", daysFromNow: 0)])
        let vm = CalendarViewModel(eventRepo: repo, userID: UserID("u1"), cityID: nil, calendar: calendar)
        await vm.loadIfNeeded()
        let firstCount = repo.fetchListCallCount
        await vm.loadIfNeeded()
        XCTAssertEqual(repo.fetchListCallCount, firstCount)
    }

    func test_refresh_bypassesCache() async {
        let repo = FakeEventRepository()
        repo.fetchListResult = .success([])
        let vm = CalendarViewModel(eventRepo: repo, userID: UserID("u1"), cityID: nil, calendar: calendar)
        await vm.refresh()
        let firstCount = repo.fetchListCallCount
        await vm.refresh()
        XCTAssertEqual(repo.fetchListCallCount, firstCount + 1)
    }

    func test_moveMonth_changesDisplayedMonthAndRefetches() async {
        let repo = FakeEventRepository()
        repo.fetchListResult = .success([])
        let vm = CalendarViewModel(eventRepo: repo, userID: UserID("u1"), cityID: nil, calendar: calendar)
        await vm.refresh()
        let originalMonth = vm.displayedMonth
        let countBefore = repo.fetchListCallCount
        await vm.moveMonth(by: 1)
        XCTAssertNotEqual(vm.displayedMonth, originalMonth)
        XCTAssertGreaterThan(repo.fetchListCallCount, countBefore)
    }
}
