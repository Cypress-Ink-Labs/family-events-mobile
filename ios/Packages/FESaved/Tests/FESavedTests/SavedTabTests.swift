import XCTest
@testable import FESaved

final class SavedTabTests: XCTestCase {
    func testTabTitle() {
        XCTAssertEqual(SavedTab().tabTitle, "Saved")
    }
}
