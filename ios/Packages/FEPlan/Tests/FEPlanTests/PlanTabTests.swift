import XCTest
import SwiftUI
@testable import FEPlan

final class PlanTabTests: XCTestCase {
    func testTabTitle() {
        XCTAssertEqual(PlanTab().tabTitle, "Plan")
    }
}
