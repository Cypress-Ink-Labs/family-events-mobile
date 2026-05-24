import Foundation
import FECore

public struct CalendarEventDTO: Equatable, Sendable, Codable, Identifiable {
    public let id: String
    public let userID: UserID
    public let eventID: EventID
    public let addedAt: Date
    public let notes: String?

    public init(
        id: String,
        userID: UserID,
        eventID: EventID,
        addedAt: Date,
        notes: String?
    ) {
        self.id = id
        self.userID = userID
        self.eventID = eventID
        self.addedAt = addedAt
        self.notes = notes
    }

    enum CodingKeys: String, CodingKey {
        case id
        case userID = "user_id"
        case eventID = "event_id"
        case addedAt = "added_at"
        case notes
    }
}

public enum CalendarEventChange: Sendable, Equatable {
    case added(CalendarEventDTO)
    case removed(eventID: EventID)
}
