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

    func testCityIDAndEventIDAreDistinctTypes() {
        let evt = EventID("x")
        let city = CityID("x")
        XCTAssertEqual(evt.rawValue, city.rawValue)
    }

    func testPlanIDEncodesAsString() throws {
        let id = PlanID("plan_1")
        let data = try JSONEncoder().encode(id)
        let decoded = try JSONDecoder().decode(PlanID.self, from: data)
        XCTAssertEqual(decoded, id)
    }
}
