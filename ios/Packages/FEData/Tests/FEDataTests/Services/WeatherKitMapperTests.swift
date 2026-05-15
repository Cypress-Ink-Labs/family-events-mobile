import XCTest
@testable import FEData

final class WeatherKitMapperTests: XCTestCase {
    func testWarmDryClear_mapsToOutdoor() {
        let snap = WeatherKitMapper.map(temperatureCelsius: 24, hourlyPrecipitationChance: 0.05, conditionCode: "Clear")
        XCTAssertEqual(snap.temperatureCelsius, 24, accuracy: 0.01)
        XCTAssertEqual(snap.precipitationChance, 0.05, accuracy: 0.001)
        XCTAssertEqual(snap.conditionCode, "clear")
        XCTAssertEqual(snap.weatherFit, "outdoor")
    }

    func testHighPrecip_mapsToIndoor() {
        let snap = WeatherKitMapper.map(temperatureCelsius: 18, hourlyPrecipitationChance: 0.8, conditionCode: "Rain")
        XCTAssertEqual(snap.precipitationChance, 0.8, accuracy: 0.001)
        XCTAssertEqual(snap.weatherFit, "indoor")
    }

    func testCold_mapsToIndoor() {
        let snap = WeatherKitMapper.map(temperatureCelsius: -3, hourlyPrecipitationChance: 0.1, conditionCode: "Snow")
        XCTAssertEqual(snap.weatherFit, "indoor")
    }

    func testHot_mapsToIndoor() {
        let snap = WeatherKitMapper.map(temperatureCelsius: 38, hourlyPrecipitationChance: 0.05, conditionCode: "MostlyClear")
        XCTAssertEqual(snap.weatherFit, "indoor")
    }

    func testMildOverThreshold_mapsToAny() {
        // Outside the strict outdoor band but inside the indoor exclusion: "any".
        let snap = WeatherKitMapper.map(temperatureCelsius: 12, hourlyPrecipitationChance: 0.1, conditionCode: "Cloudy")
        XCTAssertEqual(snap.weatherFit, "any")
    }

    func testPrecipitationChanceClampsToZeroOneRange() {
        // Apple occasionally returns precip > 1; map clamps it to 1.0 (still indoor).
        let snapHigh = WeatherKitMapper.map(temperatureCelsius: 20, hourlyPrecipitationChance: 1.5, conditionCode: "Rain")
        XCTAssertEqual(snapHigh.precipitationChance, 1.0, accuracy: 0.001)
        XCTAssertEqual(snapHigh.weatherFit, "indoor")
        // Negative -> 0.
        let snapLow = WeatherKitMapper.map(temperatureCelsius: 20, hourlyPrecipitationChance: -0.1, conditionCode: "Clear")
        XCTAssertEqual(snapLow.precipitationChance, 0.0, accuracy: 0.001)
    }
}
