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
