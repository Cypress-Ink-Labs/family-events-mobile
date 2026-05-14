import XCTest
import FECore
@testable import FEEventDetail

final class EventDetailScreenTests: XCTestCase {
    func testCarriesEventID() {
        XCTAssertEqual(EventDetailScreen(eventID: EventID("x")).eventID.rawValue, "x")
    }
}
