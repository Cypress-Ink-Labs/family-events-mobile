import XCTest
import FECore
@testable import FEData
import FEDataTesting

final class EventRepositoryProtocolTests: XCTestCase {
    func testFakeReturnsConfiguredEvents() async throws {
        let fake = FakeEventRepository()
        let dto = EventDTO.fixture(id: "evt_1", title: "Storytime")
        fake.fetchByIDsResult = .success([dto])
        let got = try await fake.fetch(ids: [EventID("evt_1")], for: UserID("u"))
        XCTAssertEqual(got.first?.title, "Storytime")
        XCTAssertEqual(fake.lastIDs, [EventID("evt_1")])
        XCTAssertEqual(fake.lastUserID, UserID("u"))
    }
}
