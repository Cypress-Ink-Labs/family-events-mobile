import XCTest
@testable import FamilyEvents

final class TabTests: XCTestCase {
    func testAllCasesExposed() {
        XCTAssertEqual(AppTab.allCases, [.plan, .explore, .map, .calendar, .saved])
    }

    func testSystemImagesAreNonEmpty() {
        for tab in AppTab.allCases {
            XCTAssertFalse(tab.systemImage.isEmpty, "tab \(tab) is missing systemImage")
        }
    }

    func testTitlesAreNonEmpty() {
        for tab in AppTab.allCases {
            XCTAssertFalse(tab.title.isEmpty, "tab \(tab) is missing title")
        }
    }

    func testRawValueMatchesId() {
        for tab in AppTab.allCases {
            XCTAssertEqual(tab.id, tab.rawValue, "id should equal rawValue for tab \(tab)")
        }
    }

    func testInitFromRawValue() {
        XCTAssertEqual(AppTab(rawValue: "plan"), .plan)
        XCTAssertEqual(AppTab(rawValue: "explore"), .explore)
        XCTAssertEqual(AppTab(rawValue: "saved"), .saved)
    }

    func testUnknownRawValueReturnsNil() {
        XCTAssertNil(AppTab(rawValue: "admin"))
        XCTAssertNil(AppTab(rawValue: ""))
    }
}
