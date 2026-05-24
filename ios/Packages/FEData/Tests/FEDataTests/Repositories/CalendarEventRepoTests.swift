import XCTest
import FECore
import FEData
@testable import FEDataTesting

@MainActor
final class FakeCalendarEventRepoTests: XCTestCase {
    func test_calendarEventsReturnsConfigured() async throws {
        let repo = FakeCalendarEventRepo()
        let dto = CalendarEventDTO(
            id: "cal_1",
            userID: UserID("u_1"),
            eventID: EventID("e_1"),
            addedAt: Date(),
            notes: nil
        )
        repo.listResult = .success([dto])
        let result = try await repo.calendarEvents(for: UserID("u_1"))
        XCTAssertEqual(result.count, 1)
        XCTAssertEqual(result.first?.id, "cal_1")
    }

    func test_addRecordsEventID() async throws {
        let repo = FakeCalendarEventRepo()
        _ = try await repo.add(eventID: EventID("e_A"), notes: "Sunny", for: UserID("u_1"))
        XCTAssertEqual(repo.addedEventIDs, [EventID("e_A")])
    }

    func test_removeRecordsEventID() async throws {
        let repo = FakeCalendarEventRepo()
        try await repo.remove(eventID: EventID("e_B"), for: UserID("u_1"))
        XCTAssertEqual(repo.removedEventIDs, [EventID("e_B")])
    }

    func test_observeYieldsNoOpStream_byDefault() {
        let repo = FakeCalendarEventRepo()
        let stream = repo.observeCalendarEvents(for: UserID("u_1"))
        XCTAssertNotNil(stream)
    }
}
