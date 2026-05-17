import XCTest
import FECore
@testable import FEEventDetail

final class EventShareURLTests: XCTestCase {
    func testBuildWithExplicitOrigin() {
        let url = EventShareURL.build(eventID: EventID("evt_123"), origin: "https://example.com")
        XCTAssertEqual(url?.absoluteString, "https://example.com/share/evt_123")
    }

    func testBuildStripsTrailingSlash() {
        let url = EventShareURL.build(eventID: EventID("evt_123"), origin: "https://example.com/")
        XCTAssertEqual(url?.absoluteString, "https://example.com/share/evt_123")
    }

    func testBuildPercentEncodesEventID() {
        let url = EventShareURL.build(eventID: EventID("a b"), origin: "https://x.test")
        XCTAssertEqual(url?.absoluteString, "https://x.test/share/a%20b")
    }

    func testBuildFallsBackToProductionOrigin() {
        let url = EventShareURL.build(eventID: EventID("evt"), origin: nil)
        XCTAssertNotNil(url)
        XCTAssertTrue(url!.absoluteString.hasSuffix("/share/evt"))
    }
}
