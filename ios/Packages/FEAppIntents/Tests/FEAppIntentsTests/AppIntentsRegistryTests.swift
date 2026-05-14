import XCTest
@testable import FEAppIntents

final class AppIntentsRegistryTests: XCTestCase {
    func testRegistryStartsEmpty() {
        XCTAssertTrue(AppIntentsRegistry.registered.isEmpty)
    }
}
