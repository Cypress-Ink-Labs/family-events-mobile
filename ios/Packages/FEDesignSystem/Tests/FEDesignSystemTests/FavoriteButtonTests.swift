import XCTest
import SwiftUI
@testable import FEDesignSystem

final class FavoriteButtonTests: XCTestCase {
    func testInitStoresBindingAndCallback() {
        var state = false
        let binding = Binding(get: { state }, set: { state = $0 })
        var tapped = false
        let button = FavoriteButton(isFavorited: binding, onToggle: { tapped = true })
        XCTAssertFalse(button.isFavorited)
        button.onToggle()
        XCTAssertTrue(tapped)
    }
}

#if os(iOS) && canImport(UIKit)
import SnapshotTesting

@MainActor
final class FavoriteButtonSnapshotTests: XCTestCase {
    func testFavoriteButtonUnfavoritedState() {
        let button = FavoriteButton(
            isFavorited: .constant(false),
            onToggle: {}
        )
        assertSnapshotVariants(of: button.padding(20), layout: .fixed(width: 120, height: 120))
    }

    func testFavoriteButtonFavoritedState() {
        let button = FavoriteButton(
            isFavorited: .constant(true),
            onToggle: {}
        )
        assertSnapshotVariants(of: button.padding(20), layout: .fixed(width: 120, height: 120))
    }
}
#endif
