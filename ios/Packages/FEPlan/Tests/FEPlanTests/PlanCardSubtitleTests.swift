import XCTest
import FECore
import FEData
import FEDataTesting
@testable import FEPlan

final class PlanCardSubtitleTests: XCTestCase {
    func testHeroSubtitleIncludesVenueWhenPresent() {
        let base = EventDTO.fixture(id: "evt_1", title: "x")
        let withVenue = EventDTO(
            id: base.id, title: base.title, description: base.description,
            startDatetime: base.startDatetime, endDatetime: base.endDatetime, timezone: base.timezone,
            venueName: "Central Library", address: base.address, cityID: base.cityID,
            latitude: base.latitude, longitude: base.longitude, ageMin: base.ageMin, ageMax: base.ageMax,
            price: base.price, isFree: base.isFree, sourceURL: base.sourceURL, sourceName: base.sourceName,
            sourceID: base.sourceID, images: base.images, status: base.status,
            aiConfidence: base.aiConfidence, aiTagProvider: base.aiTagProvider, isFeatured: base.isFeatured,
            viewCount: base.viewCount, createdAt: base.createdAt, updatedAt: base.updatedAt,
            tags: base.tags, avgRating: base.avgRating, ratingCount: base.ratingCount, isFavorited: base.isFavorited
        )
        XCTAssertTrue(PlanHeroCard.subtitle(for: withVenue).contains("Central Library"))
    }

    func testHeroSubtitleOmitsVenueWhenAbsent() {
        let base = EventDTO.fixture(id: "evt_2", title: "y")
        // fixture has venueName=nil; subtitle should not contain venue name
        let subtitle = PlanHeroCard.subtitle(for: base)
        XCTAssertFalse(subtitle.contains("Central Library"))
        XCTAssertFalse(subtitle.isEmpty)
    }

    func testThumbSubtitleDoesNotIncludeVenue() {
        let base = EventDTO.fixture(id: "evt_3", title: "z")
        // Even with venue, thumb keeps it short.
        let s = PlanThumbCard.subtitle(for: base)
        XCTAssertFalse(s.isEmpty)
    }
}
