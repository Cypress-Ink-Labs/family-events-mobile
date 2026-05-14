import XCTest
@testable import FECore

final class IdentifiersTests: XCTestCase {
    func testEventIDWrapsString() {
        let id = EventID("evt_123")
        XCTAssertEqual(id.rawValue, "evt_123")
    }

    func testEventIDIsHashable() {
        let set: Set<EventID> = [EventID("a"), EventID("b"), EventID("a")]
        XCTAssertEqual(set.count, 2)
    }

    func testTypedIdsWithSameRawValueAreSeparateAnyHashableEntries() {
        // `EventID("x")` and `CityID("x")` share a raw value but are different
        // nominal types. As AnyHashable, they must produce distinct hash
        // identities so they don't collide in heterogeneous containers.
        let bag: Set<AnyHashable> = [
            AnyHashable(EventID("x")),
            AnyHashable(CityID("x")),
        ]
        XCTAssertEqual(bag.count, 2)
    }

    func testPlanIDEncodesAsString() throws {
        let id = PlanID("plan_1")
        let data = try JSONEncoder().encode(id)
        let decoded = try JSONDecoder().decode(PlanID.self, from: data)
        XCTAssertEqual(decoded, id)
    }

    func testUserIDWrapsString() {
        let id = UserID("550e8400-e29b-41d4-a716-446655440000")
        XCTAssertEqual(id.rawValue, "550e8400-e29b-41d4-a716-446655440000")
    }

    func testUserIDRoundTripsThroughJSON() throws {
        let id = UserID("user_42")
        let data = try JSONEncoder().encode(id)
        let decoded = try JSONDecoder().decode(UserID.self, from: data)
        XCTAssertEqual(decoded, id)
    }
}
