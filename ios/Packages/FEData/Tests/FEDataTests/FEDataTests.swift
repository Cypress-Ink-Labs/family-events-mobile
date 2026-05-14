import XCTest
@testable import FEData
import FECore

final class FEDataTests: XCTestCase {
    func testPackageLoadsAndDependsOnFECore() {
        XCTAssertEqual(FEData.version, "0.1.0")
        XCTAssertEqual(FECore.version, "0.1.0")
    }
}
