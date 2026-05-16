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

    func testFakeListReturnsConfiguredEvents() async throws {
        let fake = FakeEventRepository()
        let dto = EventDTO.fixture(id: "evt_2", title: "Art Class")
        fake.fetchListResult = .success([dto])
        let query = EventQuery(cityID: CityID("city_1"), limit: 20, offset: 0)
        let got = try await fake.fetchList(query: query, for: UserID("u"))
        XCTAssertEqual(got.first?.title, "Art Class")
        XCTAssertEqual(fake.lastListQuery, query)
        XCTAssertEqual(fake.lastUserID, UserID("u"))
        XCTAssertEqual(fake.fetchListCallCount, 1)
    }

    func testFakeListCapturesQuery() async throws {
        let fake = FakeEventRepository()
        fake.fetchListResult = .success([])
        let from = Date(timeIntervalSince1970: 1_700_000_000)
        let to = Date(timeIntervalSince1970: 1_700_086_400)
        let query = EventQuery(cityID: nil, dateFrom: from, dateTo: to, limit: 5, offset: 10)
        _ = try await fake.fetchList(query: query, for: UserID("u2"))
        XCTAssertEqual(fake.lastListQuery?.limit, 5)
        XCTAssertEqual(fake.lastListQuery?.offset, 10)
        XCTAssertNil(fake.lastListQuery?.cityID)
    }

    func testEventQuerySignatureExcludesOffset() {
        let q1 = EventQuery(cityID: CityID("c1"), limit: 20, offset: 0)
        let q2 = EventQuery(cityID: CityID("c1"), limit: 20, offset: 20)
        XCTAssertEqual(q1.signature, q2.signature)
    }

    func testEventQuerySignatureDiffersOnCity() {
        let q1 = EventQuery(cityID: CityID("c1"), limit: 20, offset: 0)
        let q2 = EventQuery(cityID: CityID("c2"), limit: 20, offset: 0)
        XCTAssertNotEqual(q1.signature, q2.signature)
    }

    func testFakeListPropagatesError() async {
        let fake = FakeEventRepository()
        let boom = AppError.notFound
        fake.fetchListResult = .failure(boom)
        do {
            _ = try await fake.fetchList(query: EventQuery(), for: UserID("u"))
            XCTFail("Expected throw")
        } catch {
            XCTAssertNotNil(error)
        }
    }
}
