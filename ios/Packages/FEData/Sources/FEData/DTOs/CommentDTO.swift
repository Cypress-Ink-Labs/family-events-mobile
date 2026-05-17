import Foundation
import FECore

public struct CommentDTO: Equatable, Sendable, Codable, Identifiable {
    public let id: String
    public let userID: UserID
    public let eventID: EventID
    public let body: String
    public let isApproved: Bool
    public let isFlagged: Bool
    public let createdAt: Date
    public let updatedAt: Date
    public let authorDisplayName: String?
    public let authorAvatarURL: String?

    public init(
        id: String,
        userID: UserID,
        eventID: EventID,
        body: String,
        isApproved: Bool,
        isFlagged: Bool,
        createdAt: Date,
        updatedAt: Date,
        authorDisplayName: String? = nil,
        authorAvatarURL: String? = nil
    ) {
        self.id = id
        self.userID = userID
        self.eventID = eventID
        self.body = body
        self.isApproved = isApproved
        self.isFlagged = isFlagged
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.authorDisplayName = authorDisplayName
        self.authorAvatarURL = authorAvatarURL
    }
}
