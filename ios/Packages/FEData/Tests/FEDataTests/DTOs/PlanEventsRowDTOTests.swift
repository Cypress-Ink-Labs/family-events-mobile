import XCTest
import FECore
@testable import FEData

final class PlanEventsRowDTOTests: XCTestCase {
    func testDecodesScoredRow() throws {
        let json = """
        {
            "event_id":"evt_1",
            "score":"0.872",
            "distance_score":"0.91",
            "weather_score":"0.85",
            "age_score":"0.7",
            "history_affinity":"0.0",
            "distance_km":"3.4",
            "day_offset":"0"
        }
        """
        let row = try JSONDecoder().decode(PlanEventsRowDTO.self, from: Data(json.utf8))
        XCTAssertEqual(row.eventID, EventID("evt_1"))
        XCTAssertEqual(row.score, 0.872, accuracy: 0.001)
        XCTAssertEqual(row.dayOffset, 0)
        XCTAssertEqual(row.distanceKm, 3.4)
    }
    func testDecodesNullDistance() throws {
        let json = """
        {
            "event_id":"evt_2","score":"0.5","distance_score":"0.5","weather_score":"0.5",
            "age_score":"0.5","history_affinity":"0.0","distance_km":null,"day_offset":"2"
        }
        """
        let row = try JSONDecoder().decode(PlanEventsRowDTO.self, from: Data(json.utf8))
        XCTAssertNil(row.distanceKm)
    }
    func testDecodesNumericFields() throws {
        // PostgREST sometimes returns these as raw Double, not stringified.
        let json = """
        {
            "event_id":"evt_3","score":0.7,"distance_score":0.7,"weather_score":0.7,
            "age_score":0.7,"history_affinity":0.0,"distance_km":2.5,"day_offset":1
        }
        """
        let row = try JSONDecoder().decode(PlanEventsRowDTO.self, from: Data(json.utf8))
        XCTAssertEqual(row.score, 0.7, accuracy: 0.001)
        XCTAssertEqual(row.dayOffset, 1)
    }
}
