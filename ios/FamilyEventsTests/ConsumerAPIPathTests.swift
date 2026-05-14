import XCTest
@testable import FamilyEvents

final class ConsumerAPIPathTests: XCTestCase {
    func testConsumerPathsAreSupported() {
        XCTAssertEqual(ConsumerAPIPath.events.value, "/api/v1/events")
        XCTAssertEqual(ConsumerAPIPath.eventDetail(id: "evt_1").value, "/api/v1/events/evt_1")
        XCTAssertEqual(ConsumerAPIPath.favorites.value, "/api/v1/favorites")
        XCTAssertEqual(ConsumerAPIPath.profile.value, "/api/v1/profile")
    }

    func testAdminPathIsOutOfScope() {
        let values = [
            ConsumerAPIPath.events.value,
            ConsumerAPIPath.eventDetail(id: "evt_1").value,
            ConsumerAPIPath.favorites.value,
            ConsumerAPIPath.profile.value,
        ]
        XCTAssertFalse(values.contains { $0.contains("/admin") })
    }
}
