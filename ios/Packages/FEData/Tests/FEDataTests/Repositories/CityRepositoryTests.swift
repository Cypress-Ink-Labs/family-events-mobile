import XCTest
import FECore
@testable import FEData
import FEDataTesting

final class CityRepositoryProtocolTests: XCTestCase {
    func testFakeReturnsConfiguredCities() async throws {
        let fake = FakeCityRepository()
        fake.citiesResult = .success([
            CitySummary(id: CityID("city_aus"), name: "Austin", region: "TX"),
            CitySummary(id: CityID("city_chi"), name: "Chicago", region: "IL"),
        ])

        let got = try await fake.cities()

        XCTAssertEqual(got.map(\.name), ["Austin", "Chicago"])
    }

    func testFakeReturnsConfiguredName() async throws {
        let fake = FakeCityRepository()
        fake.nameResult = .success("Austin")
        let got = try await fake.cityName(id: CityID("city_aus"))
        XCTAssertEqual(got, "Austin")
        XCTAssertEqual(fake.lastID, CityID("city_aus"))
    }
}
