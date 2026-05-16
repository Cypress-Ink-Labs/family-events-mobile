import XCTest
import SwiftData
import FECore
import FEData
import FEDataTesting
@testable import FESaved

@MainActor
final class SavedTabTests: XCTestCase {
    func testTabTitle() throws {
        let tab = SavedTab(
            favoriteRepo: FakeFavoriteRepo(),
            eventRepo: FakeEventRepository(),
            modelContainer: try AppModelContainer.makeInMemory(),
            userID: UserID("u"),
            onOpenProfile: {}
        )
        XCTAssertEqual(tab.tabTitle, "Saved")
    }
}
