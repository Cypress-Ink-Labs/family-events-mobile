import Foundation
import FECore

public struct RatingDTO: Equatable, Sendable, Codable, Identifiable {
    public let id: String
    public let userID: UserID
    public let eventID: EventID
    public let score: Int
    public let createdAt: Date

    public init(id: String, userID: UserID, eventID: EventID, score: Int, createdAt: Date) {
        self.id = id
        self.userID = userID
        self.eventID = eventID
        self.score = score
        self.createdAt = createdAt
    }
}
