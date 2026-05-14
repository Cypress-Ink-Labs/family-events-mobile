import XCTest
import SwiftUI
@testable import FamilyEvents

@MainActor
final class RootViewSmokeTests: XCTestCase {
    func testRootViewSelectsPlanByDefault() {
        let view = RootView()
        XCTAssertEqual(view.initialTab, .plan)
    }

    func testRootViewExposesAllTabs() {
        XCTAssertEqual(RootView.shownTabs, [.plan, .explore, .saved])
    }
}
