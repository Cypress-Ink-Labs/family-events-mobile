import XCTest
import FECore
@testable import FEPlan

final class PlanContextTests: XCTestCase {
    func testHasDefaults() {
        let ctx = PlanContext(userID: UserID("u_1"), cityID: nil, kidAge: nil)
        XCTAssertEqual(ctx.userID, UserID("u_1"))
        XCTAssertNil(ctx.cityID)
        XCTAssertNil(ctx.kidAge)
    }
    func testFullyPopulated() {
        let ctx = PlanContext(userID: UserID("u_1"), cityID: CityID("city_aus"), kidAge: 5)
        XCTAssertEqual(ctx.cityID, CityID("city_aus"))
        XCTAssertEqual(ctx.kidAge, 5)
    }
    func testEquality() {
        XCTAssertEqual(
            PlanContext(userID: UserID("u"), cityID: nil, kidAge: nil),
            PlanContext(userID: UserID("u"), cityID: nil, kidAge: nil)
        )
        XCTAssertNotEqual(
            PlanContext(userID: UserID("u"), cityID: nil, kidAge: 5),
            PlanContext(userID: UserID("u"), cityID: nil, kidAge: 4)
        )
    }
}
