import XCTest
@testable import FEExplore

final class ExploreTabTests: XCTestCase {
    func testTabTitle() {
        XCTAssertEqual(ExploreTab().tabTitle, "Explore")
    }
}
