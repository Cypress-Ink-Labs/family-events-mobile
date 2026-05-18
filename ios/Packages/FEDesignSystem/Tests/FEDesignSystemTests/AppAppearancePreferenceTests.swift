import XCTest
import SwiftUI
@testable import FEDesignSystem

final class AppAppearancePreferenceTests: XCTestCase {
    func testMapsToPreferredColorScheme() {
        XCTAssertNil(AppAppearancePreference.system.preferredColorScheme)
        XCTAssertEqual(AppAppearancePreference.light.preferredColorScheme, .light)
        XCTAssertEqual(AppAppearancePreference.dark.preferredColorScheme, .dark)
    }

    func testInvalidRawValueDefaultsToSystem() {
        XCTAssertEqual(AppAppearancePreference.resolve("unknown"), .system)
        XCTAssertEqual(AppAppearancePreference.resolve("light"), .light)
    }
}
