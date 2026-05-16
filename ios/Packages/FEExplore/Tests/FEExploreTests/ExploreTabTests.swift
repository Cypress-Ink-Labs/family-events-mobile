import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEExplore

@MainActor
final class ExploreTabTests: XCTestCase {
    func testTabTitle() {
        let tab = ExploreTab(eventRepo: FakeEventRepository(), favoriteRepo: FakeFavoriteRepo(), userID: UserID("u"), cityID: nil)
        XCTAssertEqual(tab.tabTitle, "Explore")
    }
}
