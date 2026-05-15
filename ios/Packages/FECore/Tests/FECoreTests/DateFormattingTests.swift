import XCTest
@testable import FECore

final class DateFormattingTests: XCTestCase {
    func testTodayDateKeyReturnsISODate() {
        let key = DateFormatting.todayDateKey(in: TimeZone(identifier: "UTC")!)
        XCTAssertEqual(key.count, 10) // "YYYY-MM-DD"
        XCTAssertTrue(key.contains("-"))
    }
    func testAddDaysShiftsDate() {
        let result = DateFormatting.addDays(toDateKey: "2026-05-15", days: 3, in: TimeZone(identifier: "UTC")!)
        XCTAssertEqual(result, "2026-05-18")
    }
    func testAddDaysHandlesMonthBoundary() {
        let result = DateFormatting.addDays(toDateKey: "2026-05-30", days: 5, in: TimeZone(identifier: "UTC")!)
        XCTAssertEqual(result, "2026-06-04")
    }
    func testCardSubtitleFormatterProducesString() {
        // Sanity: shared formatter produces non-empty output for a fixed date.
        let date = Date(timeIntervalSince1970: 1_700_000_000) // 2023-11-14T22:13:20Z
        let out = DateFormatting.cardSubtitleFormatter.string(from: date)
        XCTAssertFalse(out.isEmpty)
    }
}
