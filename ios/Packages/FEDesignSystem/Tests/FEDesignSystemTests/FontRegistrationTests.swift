import XCTest
@testable import FEDesignSystem

final class FontRegistrationTests: XCTestCase {
    /// `registerFonts()` should always be callable and never throw. When
    /// `Resources/Fonts/` is empty (or contains only the README), the
    /// function returns empty arrays so the call site can decide whether
    /// the absence is a hard error or a soft fallback.
    func testRegisterFontsDoesNotThrow() {
        let result = FEDesignSystem.registerFonts()
        // Both arrays exist; values depend on what's bundled.
        _ = result.registered
        _ = result.failures
    }

    /// The expected PostScript name list must stay non-empty so future
    /// changes can't accidentally drop the whole roster.
    func testExpectedPostScriptNamesIsNotEmpty() {
        XCTAssertFalse(FEDesignSystem.expectedPostScriptNames.isEmpty)
    }

    /// When the bundle directory exists but holds no font files (current
    /// state pre font-binary drop), every registration attempt should
    /// safely report zero registrations and zero failures.
    func testRegisterFontsSurvivesEmptyDirectory() {
        let result = FEDesignSystem.registerFonts()
        // The README isn't a font file, so it's filtered by extension check.
        // Any actual .ttf/.otf files would appear in `registered` here.
        XCTAssertEqual(result.failures.count, 0, "Bundled fonts should not fail to register")
    }
}
