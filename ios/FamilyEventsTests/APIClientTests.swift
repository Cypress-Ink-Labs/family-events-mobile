import XCTest
@testable import FamilyEvents

final class APIClientTests: XCTestCase {
    func testBuildsURLFromConsumerPath() {
        let client = APIClient(baseURL: URL(string: "https://example.com")!)
        let url = client.url(for: .events)
        XCTAssertEqual(url.absoluteString, "https://example.com/api/v1/events")
    }
}
