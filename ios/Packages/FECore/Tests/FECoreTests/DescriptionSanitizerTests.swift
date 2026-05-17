import XCTest
@testable import FECore

final class DescriptionSanitizerTests: XCTestCase {
    func testNilAndEmpty() {
        XCTAssertNil(DescriptionSanitizer.clean(nil))
        XCTAssertNil(DescriptionSanitizer.clean(""))
        XCTAssertNil(DescriptionSanitizer.clean("   \n\t  "))
    }

    func testStripsDiviOpeningShortcode() {
        let input = #"[et_pb_section fb_built="1" _builder_version="4.16"]Hello"#
        XCTAssertEqual(DescriptionSanitizer.clean(input), "Hello")
    }

    func testStripsDiviClosingShortcode() {
        let input = "Hello[/et_pb_section]"
        XCTAssertEqual(DescriptionSanitizer.clean(input), "Hello")
    }

    func testStripsRockTheBlockFixture() {
        let input = #"""
        [et_pb_section fb_built="1" _builder_version="4.16" global_colors_info="{}"][et_pb_row column_structure="2_5,3_5" _builder_version="4.27.6" background_size="initial" background_position="top_left" background_repeat="repeat" global_colors_info="{}"][et_pb_column type="2_5" _builder_version="4.16"][et_pb_image src="https://example.org/img.png" title_text="Rock the Block"][/et_pb_image][/et_pb_column][/et_pb_row][/et_pb_section]
        Welcome to Rock the Block!
        """#
        let cleaned = DescriptionSanitizer.clean(input) ?? ""
        XCTAssertFalse(cleaned.contains("et_pb"))
        XCTAssertFalse(cleaned.contains("["))
        XCTAssertTrue(cleaned.contains("Welcome to Rock the Block!"))
    }

    func testStripsGenericShortcodes() {
        let input = "[caption id=\"a\"]My caption[/caption] text"
        let cleaned = DescriptionSanitizer.clean(input) ?? ""
        XCTAssertFalse(cleaned.contains("[caption"))
        XCTAssertTrue(cleaned.contains("My caption"))
    }

    func testConvertsBrAndPToNewlines() {
        let input = "<p>Line one</p><p>Line two</p>Line<br>three"
        let cleaned = DescriptionSanitizer.clean(input) ?? ""
        XCTAssertTrue(cleaned.contains("Line one"))
        XCTAssertTrue(cleaned.contains("Line two"))
        XCTAssertTrue(cleaned.contains("Line\nthree"))
    }

    func testStripsAllHTMLTags() {
        let input = "<strong>Bold</strong> and <em>italic</em>"
        XCTAssertEqual(DescriptionSanitizer.clean(input), "Bold and italic")
    }

    func testDecodesCommonEntities() {
        let input = "Tom&nbsp;&amp;&nbsp;Jerry&rsquo;s show &hellip;"
        XCTAssertEqual(DescriptionSanitizer.clean(input), "Tom & Jerry's show …")
    }

    func testCollapsesBlankLines() {
        let input = "Line 1\n\n\n\n\nLine 2"
        XCTAssertEqual(DescriptionSanitizer.clean(input), "Line 1\n\nLine 2")
    }

    func testPreservesPlainText() {
        let input = "A normal sentence. Another one."
        XCTAssertEqual(DescriptionSanitizer.clean(input), "A normal sentence. Another one.")
    }

    func testStripsTrailingUnclosedDiviShortcode() {
        // Mirrors the 500-char-truncated Rock the Block DB row: ingest used
        // to slice raw description before cleaning, so the final shortcode
        // arrives without its closing `]`.
        let input = #"[et_pb_section fb_built="1"]Hello[et_pb_image src="https://example.org/img.png" title_text="Rock the Block" "#
        XCTAssertEqual(DescriptionSanitizer.clean(input), "Hello")
    }

    func testStripsTrailingUnclosedGenericShortcodeButKeepsUnclosedProse() {
        XCTAssertEqual(DescriptionSanitizer.clean(#"Hello[caption id="x""#), "Hello")
        // Unclosed prose with no attributes should survive — only an
        // attribute (`name <space> ...`) suggests it's a shortcode.
        XCTAssertEqual(DescriptionSanitizer.clean("Hello [See details"), "Hello [See details")
    }
}
