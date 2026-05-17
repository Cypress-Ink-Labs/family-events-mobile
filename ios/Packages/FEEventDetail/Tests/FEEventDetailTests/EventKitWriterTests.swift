import XCTest
import FEData
import FECore
import FEDataTesting
@testable import FEEventDetail

final class EventKitWriterTests: XCTestCase {
    func testLocationCombinesVenueAndAddress() {
        let event = makeEvent(venue: "Civic Center", address: "123 Main St")
        XCTAssertEqual(EventKitWriter.locationString(for: event), "Civic Center, 123 Main St")
    }

    func testLocationUsesVenueWhenAddressEmpty() {
        let event = makeEvent(venue: "Civic Center", address: nil)
        XCTAssertEqual(EventKitWriter.locationString(for: event), "Civic Center")
    }

    func testLocationUsesAddressWhenVenueEmpty() {
        let event = makeEvent(venue: nil, address: "123 Main St")
        XCTAssertEqual(EventKitWriter.locationString(for: event), "123 Main St")
    }

    func testLocationNilWhenBothEmpty() {
        let event = makeEvent(venue: nil, address: nil)
        XCTAssertNil(EventKitWriter.locationString(for: event))
    }

    func testNotesIncludesDescriptionAndSource() {
        let event = makeEvent(description: "Bring a blanket.", sourceURL: "https://example.org/e/1")
        let notes = EventKitWriter.notesString(for: event)
        XCTAssertNotNil(notes)
        XCTAssertTrue(notes!.contains("Bring a blanket."))
        XCTAssertTrue(notes!.contains("Source: https://example.org/e/1"))
    }

    func testNotesNilWhenBothEmpty() {
        let event = makeEvent(description: nil, sourceURL: nil)
        XCTAssertNil(EventKitWriter.notesString(for: event))
    }

    private func makeEvent(
        venue: String? = nil,
        address: String? = nil,
        description: String? = nil,
        sourceURL: String? = nil
    ) -> EventDTO {
        let base = EventDTO.fixture(id: "e", title: "t")
        return EventDTO(
            id: base.id, title: base.title, description: description,
            startDatetime: base.startDatetime, endDatetime: base.endDatetime, timezone: base.timezone,
            venueName: venue, address: address, cityID: base.cityID,
            latitude: base.latitude, longitude: base.longitude, ageMin: base.ageMin, ageMax: base.ageMax,
            price: base.price, isFree: base.isFree, sourceURL: sourceURL, sourceName: base.sourceName,
            sourceID: base.sourceID, images: base.images, status: base.status,
            aiConfidence: base.aiConfidence, aiTagProvider: base.aiTagProvider, isFeatured: base.isFeatured,
            viewCount: base.viewCount, createdAt: base.createdAt, updatedAt: base.updatedAt,
            tags: base.tags, avgRating: base.avgRating, ratingCount: base.ratingCount, isFavorited: base.isFavorited
        )
    }
}
