import XCTest
import FECore
@testable import FEData

final class EventDTOTests: XCTestCase {
    func testDecodesEnrichedRow() throws {
        let json = """
        {
            "id": "evt_1",
            "title": "Sunday Storytime",
            "description": "Family-friendly storytime at the library.",
            "start_datetime": "2026-05-17T15:00:00Z",
            "end_datetime": "2026-05-17T16:00:00Z",
            "timezone": "America/Chicago",
            "venue_name": "Central Library",
            "address": "800 Guadalupe St",
            "city_id": "city_aus",
            "latitude": 30.27,
            "longitude": -97.74,
            "age_min": 3,
            "age_max": 8,
            "price": 0,
            "is_free": true,
            "source_url": "https://example.com/event/1",
            "source_name": "Library Programs",
            "source_id": null,
            "images": ["https://example.com/img.jpg"],
            "status": "published",
            "ai_confidence": 0.94,
            "ai_tag_provider": "openai",
            "is_featured": false,
            "view_count": 42,
            "created_at": "2026-05-10T12:00:00Z",
            "updated_at": "2026-05-12T12:00:00Z",
            "tags": [
                {"id":"t1","name":"Family","slug":"family","color":"#abc"}
            ],
            "avg_rating": 4.5,
            "rating_count": 12,
            "is_favorited": false
        }
        """
        let dto = try JSONDecoder().decode(EventDTO.self, from: Data(json.utf8))
        XCTAssertEqual(dto.id, EventID("evt_1"))
        XCTAssertEqual(dto.title, "Sunday Storytime")
        XCTAssertEqual(dto.tags.count, 1)
        XCTAssertEqual(dto.tags.first?.slug, "family")
        XCTAssertTrue(dto.isFree)
        XCTAssertEqual(dto.images.count, 1)
        XCTAssertEqual(dto.avgRating, 4.5, accuracy: 0.01)
    }
    func testHandlesNullImagesAsEmptyArray() throws {
        let json = """
        {
            "id":"x","title":"x","description":null,"start_datetime":"2026-01-01T00:00:00Z",
            "end_datetime":null,"timezone":"UTC","venue_name":null,"address":null,
            "city_id":null,"latitude":null,"longitude":null,"age_min":null,"age_max":null,
            "price":null,"is_free":false,"source_url":null,"source_name":null,"source_id":null,
            "images":null,"status":"published","ai_confidence":null,"ai_tag_provider":null,
            "is_featured":false,"view_count":0,"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z","tags":[]
        }
        """
        let dto = try JSONDecoder().decode(EventDTO.self, from: Data(json.utf8))
        XCTAssertEqual(dto.images.count, 0)
        XCTAssertEqual(dto.avgRating, 0)
    }
    func testDecodesV2FieldsWhenPresent() throws {
        let json = """
        {
            "id":"evt_v2","title":"Outdoor Play","description":null,
            "start_datetime":"2026-06-01T10:00:00Z","end_datetime":null,
            "timezone":"UTC","venue_name":null,"address":null,"city_id":null,
            "latitude":null,"longitude":null,"age_min":null,"age_max":null,
            "price":null,"is_free":true,"source_url":null,"source_name":null,"source_id":null,
            "images":[],"status":"published","ai_confidence":null,"ai_tag_provider":null,
            "is_featured":false,"view_count":0,
            "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z",
            "tags":[],
            "is_outdoor": true,
            "parent_tips": [{"category": "Fun", "text": "Good for toddlers"}]
        }
        """
        let dto = try JSONDecoder().decode(EventDTO.self, from: Data(json.utf8))
        XCTAssertEqual(dto.isOutdoor, true)
        XCTAssertEqual(dto.parentTips?.first?.category, "Fun")
        XCTAssertEqual(dto.parentTips?.first?.text, "Good for toddlers")
    }

    func testDecodesV2FieldsWhenAbsent() throws {
        let json = """
        {
            "id":"evt_v2_absent","title":"Indoor Story","description":null,
            "start_datetime":"2026-06-01T10:00:00Z","end_datetime":null,
            "timezone":"UTC","venue_name":null,"address":null,"city_id":null,
            "latitude":null,"longitude":null,"age_min":null,"age_max":null,
            "price":null,"is_free":true,"source_url":null,"source_name":null,"source_id":null,
            "images":[],"status":"published","ai_confidence":null,"ai_tag_provider":null,
            "is_featured":false,"view_count":0,
            "created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z",
            "tags":[]
        }
        """
        let dto = try JSONDecoder().decode(EventDTO.self, from: Data(json.utf8))
        XCTAssertNil(dto.isOutdoor)
        XCTAssertNil(dto.parentTips)
    }

    func testMalformedDateRaisesDecodingError() {
        let json = """
        {
            "id":"x","title":"x","description":null,"start_datetime":"not-a-date",
            "end_datetime":null,"timezone":"UTC","venue_name":null,"address":null,
            "city_id":null,"latitude":null,"longitude":null,"age_min":null,"age_max":null,
            "price":null,"is_free":false,"source_url":null,"source_name":null,"source_id":null,
            "images":null,"status":"published","ai_confidence":null,"ai_tag_provider":null,
            "is_featured":false,"view_count":0,"created_at":"2026-01-01T00:00:00Z","updated_at":"2026-01-01T00:00:00Z","tags":[]
        }
        """
        XCTAssertThrowsError(try JSONDecoder().decode(EventDTO.self, from: Data(json.utf8))) { error in
            XCTAssertTrue(error is DecodingError, "Expected DecodingError, got \(type(of: error))")
        }
    }
}
