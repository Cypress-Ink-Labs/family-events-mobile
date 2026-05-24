import XCTest
@testable import FESaved

final class SavedFilterTests: XCTestCase {
    func test_all_includesEverything() {
        let now = Date(timeIntervalSince1970: 1_000_000)
        let past = now.addingTimeInterval(-3600)
        let future = now.addingTimeInterval(3600)
        XCTAssertTrue(SavedFilter.all.includes(eventStart: past, now: now))
        XCTAssertTrue(SavedFilter.all.includes(eventStart: future, now: now))
    }

    func test_upcoming_excludesPast() {
        let now = Date(timeIntervalSince1970: 1_000_000)
        let past = now.addingTimeInterval(-3600)
        let future = now.addingTimeInterval(3600)
        XCTAssertFalse(SavedFilter.upcoming.includes(eventStart: past, now: now))
        XCTAssertTrue(SavedFilter.upcoming.includes(eventStart: future, now: now))
        XCTAssertTrue(SavedFilter.upcoming.includes(eventStart: now, now: now), "now is included")
    }

    func test_past_excludesUpcoming() {
        let now = Date(timeIntervalSince1970: 1_000_000)
        let past = now.addingTimeInterval(-3600)
        let future = now.addingTimeInterval(3600)
        XCTAssertTrue(SavedFilter.past.includes(eventStart: past, now: now))
        XCTAssertFalse(SavedFilter.past.includes(eventStart: future, now: now))
    }

    func test_allCases_covers3() {
        XCTAssertEqual(SavedFilter.allCases.count, 3)
    }
}
