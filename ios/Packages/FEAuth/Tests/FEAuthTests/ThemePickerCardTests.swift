import XCTest
import SwiftUI
import FEDesignSystem
@testable import FEAuth

@MainActor
final class ThemePickerCardTests: XCTestCase {
    func test_appearanceRoundTrip_light() {
        var stored = AppAppearancePreference.system.rawValue
        let binding = Binding<String>(
            get: { stored },
            set: { stored = $0 }
        )
        let card = ThemePickerCard(appearanceRawValue: binding)
        // Drive through the binding the way the SwiftUI picker would.
        card.appearanceRawValue = AppAppearancePreference.light.rawValue
        XCTAssertEqual(stored, AppAppearancePreference.light.rawValue)
    }

    func test_resolveAppAppearance_fallsBackToSystem_forUnknownValue() {
        XCTAssertEqual(AppAppearancePreference.resolve("garbage"), .system)
    }

    func test_resolveAppAppearance_preserves_validValues() {
        for option in AppAppearancePreference.allCases {
            XCTAssertEqual(AppAppearancePreference.resolve(option.rawValue), option)
        }
    }
}
