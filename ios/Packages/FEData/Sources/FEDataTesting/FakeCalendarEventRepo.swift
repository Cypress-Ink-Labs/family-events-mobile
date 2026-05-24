import Foundation
import FECore
import FEData

public final class FakeCalendarEventRepo: CalendarEventRepo, @unchecked Sendable {
    public var listResult: Result<[CalendarEventDTO], Error> = .success([])
    public var addError: Error?
    public var removeError: Error?
    public private(set) var addedEventIDs: [EventID] = []
    public private(set) var removedEventIDs: [EventID] = []

    public init() {}

    public func calendarEvents(for userID: UserID) async throws -> [CalendarEventDTO] {
        try listResult.get()
    }

    public func add(eventID: EventID, notes: String?, for userID: UserID) async throws -> CalendarEventDTO {
        if let addError { throw addError }
        addedEventIDs.append(eventID)
        return CalendarEventDTO(
            id: UUID().uuidString,
            userID: userID,
            eventID: eventID,
            addedAt: Date(),
            notes: notes
        )
    }

    public func remove(eventID: EventID, for userID: UserID) async throws {
        if let removeError { throw removeError }
        removedEventIDs.append(eventID)
    }
}
