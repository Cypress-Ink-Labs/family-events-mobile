import XCTest
@testable import FamilyEvents

final class TabTests: XCTestCase {
    func testAllCasesExposed() {
        XCTAssertEqual(AppTab.allCases, [.plan, .explore, .saved, .admin])
    }

    func testSystemImagesAreNonEmpty() {
        for tab in AppTab.allCases {
            XCTAssertFalse(tab.systemImage.isEmpty, "tab \(tab) is missing systemImage")
        }
    }
}
