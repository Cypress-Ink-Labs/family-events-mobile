import XCTest
import FECore
@testable import FEData

final class LocationAuthorizationStatusTests: XCTestCase {
    func testStatusHasExpectedCases() {
        let cases: [LocationAuthorizationStatus] = [.notDetermined, .denied, .restricted, .authorized]
        XCTAssertEqual(cases.count, 4)
    }
}
