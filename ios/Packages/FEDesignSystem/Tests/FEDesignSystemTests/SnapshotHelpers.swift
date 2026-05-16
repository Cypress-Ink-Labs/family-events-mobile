#if os(iOS) && canImport(UIKit)
import XCTest
import SwiftUI
import SnapshotTesting

/// Asserts three snapshots per view: light, dark, and Dynamic Type XXL.
/// All use a fixed canvas to neutralize simulator size drift across Xcode
/// versions; the canvas is wider than any real iPhone so wrapping behavior
/// is captured but device-specific safe-area shifts are not.
@MainActor
func assertSnapshotVariants<V: View>(
    of view: V,
    layout: SwiftUISnapshotLayout = .fixed(width: 390, height: 700),
    file: StaticString = #filePath,
    testName: String = #function,
    line: UInt = #line
) {
    assertSnapshot(
        of: view,
        as: .image(layout: layout, traits: UITraitCollection(userInterfaceStyle: .light)),
        named: "light",
        file: file, testName: testName, line: line
    )
    assertSnapshot(
        of: view,
        as: .image(layout: layout, traits: UITraitCollection(userInterfaceStyle: .dark)),
        named: "dark",
        file: file, testName: testName, line: line
    )
    assertSnapshot(
        of: view,
        as: .image(
            layout: layout,
            traits: UITraitCollection(traitsFrom: [
                UITraitCollection(preferredContentSizeCategory: .accessibilityExtraExtraLarge),
                UITraitCollection(userInterfaceStyle: .light),
            ])
        ),
        named: "xxl-type",
        file: file, testName: testName, line: line
    )
}
#endif
