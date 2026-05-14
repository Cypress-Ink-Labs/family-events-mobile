import XCTest
import SwiftUI
@testable import FEDesignSystem

final class PlaceholderViewTests: XCTestCase {
    func testPlaceholderViewExposesTitleAndSymbol() {
        let view = PlaceholderView(title: "Plan", systemImage: "calendar.badge.clock")
        XCTAssertEqual(view.title, "Plan")
        XCTAssertEqual(view.systemImage, "calendar.badge.clock")
    }

    func testAppTypographyExposesTitleStyle() {
        let style = AppTypography.titleLarge
        XCTAssertNotNil(style)
    }
}
