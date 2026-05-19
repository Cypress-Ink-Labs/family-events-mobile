import XCTest
@testable import FEAdmin

final class FEAdminTests: XCTestCase {
    func testAdminSectionsHasTenEntries() {
        XCTAssertEqual(adminSections().count, 10)
    }
}
