import XCTest
import FECore
@testable import FEData
import FEDataTesting

final class CityRepositoryProtocolTests: XCTestCase {
    func testFakeReturnsConfiguredName() async throws {
        let fake = FakeCityRepository()
        fake.nameResult = .success("Austin")
        let got = try await fake.cityName(id: CityID("city_aus"))
        XCTAssertEqual(got, "Austin")
        XCTAssertEqual(fake.lastID, CityID("city_aus"))
    }
}
