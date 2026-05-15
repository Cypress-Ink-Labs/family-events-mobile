import XCTest
import SwiftUI
@testable import FEDesignSystem

final class EventCardTests: XCTestCase {
    func testStoresProperties() {
        let card = EventCard(title: "Storytime", subtitle: "Today, 3:00 PM", imageURL: nil, badge: "Free")
        XCTAssertEqual(card.title, "Storytime")
        XCTAssertEqual(card.subtitle, "Today, 3:00 PM")
        XCTAssertEqual(card.badge, "Free")
        XCTAssertNil(card.imageURL)
    }
    func testNilBadgeIsAccepted() {
        let card = EventCard(title: "x", subtitle: "y")
        XCTAssertNil(card.badge)
    }
    func testOnTapClosureIsRetained() {
        var tapped = false
        let card = EventCard(title: "x", subtitle: "y", onTap: { tapped = true })
        card.onTap?()
        XCTAssertTrue(tapped)
    }
}
