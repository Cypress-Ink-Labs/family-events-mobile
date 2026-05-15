import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEEventDetail

@MainActor
final class EventDetailScreenTests: XCTestCase {
    func testConstructsWithoutCrash() {
        let repo = FakeEventRepository()
        _ = EventDetailScreen(eventID: EventID("evt_1"), eventRepo: repo, userID: UserID("u"))
    }
}
