import XCTest
@testable import FECore

final class ConsumerAPIPathTests: XCTestCase {
    func testConsumerPathsAreSupported() {
        XCTAssertEqual(ConsumerAPIPath.events.value, "/api/v1/events")
        XCTAssertEqual(ConsumerAPIPath.eventDetail(id: EventID("evt_1")).value, "/api/v1/events/evt_1")
        XCTAssertEqual(ConsumerAPIPath.favorites.value, "/api/v1/favorites")
        XCTAssertEqual(ConsumerAPIPath.profile.value, "/api/v1/profile")
    }

    func testNoAdminPathsExposed() {
        let allCases: [ConsumerAPIPath] = [
            .events,
            .eventDetail(id: EventID("evt_1")),
            .favorites,
            .profile,
        ]
        for path in allCases {
            XCTAssertFalse(path.value.contains("/admin"), "admin path leaked into ConsumerAPIPath: \(path.value)")
        }
    }
}
