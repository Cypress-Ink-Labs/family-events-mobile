import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEExplore

@MainActor
final class ExploreTabTests: XCTestCase {
    func testTabTitle() {
        let tab = ExploreTab(eventRepo: FakeEventRepository(), userID: UserID("u"), cityID: nil)
        XCTAssertEqual(tab.tabTitle, "Explore")
    }
}
