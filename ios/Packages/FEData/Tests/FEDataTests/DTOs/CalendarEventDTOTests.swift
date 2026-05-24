import XCTest
import FECore
@testable import FEData

final class CalendarEventDTOTests: XCTestCase {
    func test_roundTripsThroughJSON_withSnakeCaseKeys() throws {
        let dto = CalendarEventDTO(
            id: "cal_1",
            userID: UserID("u_1"),
            eventID: EventID("e_1"),
            addedAt: Date(timeIntervalSince1970: 1_700_000_000),
            notes: "Bring sunscreen"
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        let data = try encoder.encode(dto)
        let json = try XCTUnwrap(String(data: data, encoding: .utf8))
        XCTAssertTrue(json.contains("\"user_id\""))
        XCTAssertTrue(json.contains("\"event_id\""))
        XCTAssertTrue(json.contains("\"added_at\""))

        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let decoded = try decoder.decode(CalendarEventDTO.self, from: data)
        XCTAssertEqual(decoded, dto)
    }

    func test_changeEquality() {
        let dto = CalendarEventDTO(
            id: "cal_1",
            userID: UserID("u_1"),
            eventID: EventID("e_1"),
            addedAt: Date(timeIntervalSince1970: 0),
            notes: nil
        )
        XCTAssertEqual(
            CalendarEventChange.added(dto),
            CalendarEventChange.added(dto)
        )
        XCTAssertNotEqual(
            CalendarEventChange.added(dto),
            CalendarEventChange.removed(eventID: dto.eventID)
        )
    }
}
