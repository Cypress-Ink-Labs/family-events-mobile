import XCTest
import FECore
@testable import FEData
import FEDataTesting

final class PlanRepositoryProtocolTests: XCTestCase {
    func testFakeReturnsConfiguredRanking() async throws {
        let fake = FakePlanRepository()
        fake.fetchPlanResult = .success([
            PlanEventsRowDTO(eventID: EventID("evt_1"), score: 0.9, distanceScore: 0.9, weatherScore: 0.9, ageScore: 0.9, historyAffinity: 0, distanceKm: 1.0, dayOffset: 0)
        ])
        let input = PlanInput(userID: UserID("u"), date: "2026-05-15", cityID: nil, coordinate: nil, kidAge: 5, weatherFit: "any", limit: 3, maxDays: 7)
        let got = try await fake.fetchPlan(input: input)
        XCTAssertEqual(got.first?.score, 0.9)
        XCTAssertEqual(fake.lastInput, input)
        XCTAssertEqual(fake.callCount, 1)
    }
}
