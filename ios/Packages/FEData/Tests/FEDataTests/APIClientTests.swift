import XCTest
@testable import FEData
import FECore

final class APIClientTests: XCTestCase {
    func testBuildsURLFromConsumerPath() {
        let client = APIClient(baseURL: URL(string: "https://example.com")!)
        let url = client.url(for: .events)
        XCTAssertEqual(url.absoluteString, "https://example.com/api/v1/events")
    }

    func testEventDetailURLEmbedsTypedID() {
        let client = APIClient(baseURL: URL(string: "https://example.com")!)
        let url = client.url(for: .eventDetail(id: EventID("evt_42")))
        XCTAssertEqual(url.absoluteString, "https://example.com/api/v1/events/evt_42")
    }
}
