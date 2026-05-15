import XCTest
@testable import FEData

final class WeatherSnapshotTests: XCTestCase {
    func testWeatherFitDerivation() {
        XCTAssertEqual(WeatherSnapshot(temperatureCelsius: 22, precipitationChance: 0.05, conditionCode: "clear").weatherFit, "outdoor")
        XCTAssertEqual(WeatherSnapshot(temperatureCelsius: 18, precipitationChance: 0.45, conditionCode: "rain").weatherFit, "indoor")
        XCTAssertEqual(WeatherSnapshot(temperatureCelsius: -2, precipitationChance: 0.1, conditionCode: "snow").weatherFit, "indoor")
        XCTAssertEqual(WeatherSnapshot(temperatureCelsius: 30, precipitationChance: 0.0, conditionCode: "clear").weatherFit, "outdoor")
    }
}
