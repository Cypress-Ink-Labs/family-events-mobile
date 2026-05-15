import XCTest
import FECore
@testable import FEData
import FEDataTesting

final class ProfileRepoProtocolTests: XCTestCase {
    func testFakeReturnsConfiguredContext() async throws {
        let fake = FakeProfileRepo()
        fake.contextResult = .success((cityID: CityID("city_aus"), kidAge: 5))
        let got = try await fake.currentContext(userID: UserID("u_1"))
        XCTAssertEqual(got.cityID, CityID("city_aus"))
        XCTAssertEqual(got.kidAge, 5)
        XCTAssertEqual(fake.lastUserID, UserID("u_1"))
    }

    func testFakeReturnsEmptyContext() async throws {
        let fake = FakeProfileRepo()
        let got = try await fake.currentContext(userID: UserID("u_no_profile"))
        XCTAssertNil(got.cityID)
        XCTAssertNil(got.kidAge)
    }
}
