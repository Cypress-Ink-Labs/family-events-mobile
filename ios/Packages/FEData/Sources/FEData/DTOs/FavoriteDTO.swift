import Foundation
import FECore

public struct FavoriteDTO: Equatable, Sendable, Codable {
    public let id: String
    public let userID: UserID
    public let eventID: EventID
    public let createdAt: Date

    public init(id: String, userID: UserID, eventID: EventID, createdAt: Date) {
        self.id = id
        self.userID = userID
        self.eventID = eventID
        self.createdAt = createdAt
    }

    enum CodingKeys: String, CodingKey {
        case id
        case userID = "user_id"
        case eventID = "event_id"
        case createdAt = "created_at"
    }
}

public enum FavoriteChange: Sendable, Equatable {
    case inserted(FavoriteDTO)
    case deleted(eventID: EventID)
}
