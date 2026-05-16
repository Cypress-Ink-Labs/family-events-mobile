import XCTest
import SwiftUI
import SwiftData
import FECore
import FEData
import FEDataTesting
@testable import FEPlan

@MainActor
final class PlanTabTests: XCTestCase {
    private func makeComposer() throws -> PlanComposer {
        PlanComposer(
            location: FakeLocationService(),
            weather: FakeWeatherProviding(),
            planRepo: FakePlanRepository(),
            eventRepo: FakeEventRepository(),
            modelContainer: try AppModelContainer.makeInMemory()
        )
    }

    func testTabTitle() throws {
        let tab = PlanTab(
            composer: try makeComposer(),
            eventRepo: FakeEventRepository(),
            favoriteRepo: FakeFavoriteRepo(),
            context: PlanContext(userID: UserID("u"))
        )
        XCTAssertEqual(tab.tabTitle, "Plan")
    }
}
