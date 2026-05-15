import XCTest
import FECore
@testable import FEData
import FEDataTesting

final class WeatherProvidingProtocolTests: XCTestCase {
    func testFakeReturnsConfiguredSnapshot() async throws {
        let fake = FakeWeatherProviding()
        let snapshot = WeatherSnapshot(temperatureCelsius: 22, precipitationChance: 0.1, conditionCode: "clear")
        fake.snapshotStub = snapshot
        let got = try await fake.currentWeather(at: GeoCoordinate(latitude: 30, longitude: -97))
        XCTAssertEqual(got, snapshot)
    }
    func testFakeThrowsWhenConfigured() async {
        let fake = FakeWeatherProviding()
        struct Boom: Error {}
        fake.errorStub = Boom()
        do {
            _ = try await fake.currentWeather(at: GeoCoordinate(latitude: 30, longitude: -97))
            XCTFail("expected throw")
        } catch is Boom {
            // ok
        } catch {
            XCTFail("wrong error type: \(error)")
        }
    }
}
