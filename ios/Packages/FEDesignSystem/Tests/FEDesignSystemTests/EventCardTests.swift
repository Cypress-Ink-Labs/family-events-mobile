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

#if os(iOS) && canImport(UIKit)
import SnapshotTesting

@MainActor
final class EventCardSnapshotTests: XCTestCase {
    override class func setUp() {
        super.setUp()
        // Set this to true to regenerate baselines. Commit baselines after,
        // then flip back to false so CI catches regressions.
        // isRecording = true
    }

    func testEventCardWithImageAndBadge() {
        let card = EventCard(
            title: "Storytime at the Library",
            subtitle: "Sat, May 16 · 10:00 AM · Central Library",
            imageURL: nil,
            badge: "Free"
        )
        assertSnapshotVariants(of: card.padding(16))
    }

    func testEventCardWithoutBadge() {
        let card = EventCard(
            title: "Family Yoga in the Park",
            subtitle: "Sun, May 17 · 9:00 AM · Moncus Park",
            imageURL: nil,
            badge: nil
        )
        assertSnapshotVariants(of: card.padding(16))
    }

    func testEventCardLongTitleWraps() {
        let card = EventCard(
            title: "An Exceptionally Long Event Title That Should Wrap Across Multiple Lines To Verify Layout Stability",
            subtitle: "Mon, May 18 · 6:00 PM",
            imageURL: nil,
            badge: "Free"
        )
        assertSnapshotVariants(of: card.padding(16))
    }
}
#endif
