import XCTest
@testable import FECore

final class FECoreTests: XCTestCase {
    func testPackageLoads() {
        XCTAssertEqual(FECore.version, "0.1.0")
    }
}
