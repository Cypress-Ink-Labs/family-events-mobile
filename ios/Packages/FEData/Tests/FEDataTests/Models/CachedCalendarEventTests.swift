import XCTest
import SwiftData
@testable import FEData

final class CachedCalendarEventTests: XCTestCase {
    func test_compositeKey_combinesUserAndEvent() {
        let row = CachedCalendarEvent(
            userID: "u_1",
            eventID: "e_1",
            addedAt: Date(timeIntervalSince1970: 0),
            notes: nil,
            lastSyncedAt: Date(timeIntervalSince1970: 0)
        )
        XCTAssertEqual(row.compositeKey, "u_1::e_1")
    }

    func test_compositeKeyIsUnique_acrossDifferentEvents() {
        let a = CachedCalendarEvent(userID: "u", eventID: "x", addedAt: Date(), notes: nil, lastSyncedAt: Date())
        let b = CachedCalendarEvent(userID: "u", eventID: "y", addedAt: Date(), notes: nil, lastSyncedAt: Date())
        XCTAssertNotEqual(a.compositeKey, b.compositeKey)
    }
}

final class CachedCityTests: XCTestCase {
    func test_sortKey_isLowercased() {
        let city = CachedCity(id: "c_1", name: "San Francisco", stateCode: "CA", lastSyncedAt: Date())
        XCTAssertEqual(city.sortKey, "san francisco")
    }
}
