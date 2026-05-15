import XCTest
@testable import FECore

final class GeoCoordinateTests: XCTestCase {
    func testStoresLatLng() {
        let c = GeoCoordinate(latitude: 30.27, longitude: -97.74)
        XCTAssertEqual(c.latitude, 30.27, accuracy: 0.001)
        XCTAssertEqual(c.longitude, -97.74, accuracy: 0.001)
    }
    func testCodableRoundTrip() throws {
        let c = GeoCoordinate(latitude: 1, longitude: 2)
        let data = try JSONEncoder().encode(c)
        let decoded = try JSONDecoder().decode(GeoCoordinate.self, from: data)
        XCTAssertEqual(decoded, c)
    }
    func testIsValidRejectsOutOfRange() {
        XCTAssertTrue(GeoCoordinate(latitude: 0, longitude: 0).isValid)
        XCTAssertFalse(GeoCoordinate(latitude: 91, longitude: 0).isValid)
        XCTAssertFalse(GeoCoordinate(latitude: 0, longitude: 181).isValid)
    }
}
