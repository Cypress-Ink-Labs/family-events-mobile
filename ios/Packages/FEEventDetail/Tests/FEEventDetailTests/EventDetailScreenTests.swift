import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEEventDetail

@MainActor
final class EventDetailScreenTests: XCTestCase {
    func testConstructsWithoutCrash() {
        _ = EventDetailScreen(
            eventID: EventID("evt_1"),
            eventRepo: FakeEventRepository(),
            favoriteRepo: FakeFavoriteRepo(),
            userID: UserID("u")
        )
    }
}
