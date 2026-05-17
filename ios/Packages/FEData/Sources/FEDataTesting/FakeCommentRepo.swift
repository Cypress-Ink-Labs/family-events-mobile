import Foundation
import FECore
import FEData

public final class FakeCommentRepo: CommentRepo, @unchecked Sendable {
    public var commentsResult: [CommentDTO] = []
    public var commentsError: Error?
    public var addError: Error?
    public private(set) var addedBodies: [String] = []

    public init(comments: [CommentDTO] = []) {
        self.commentsResult = comments
    }

    public func comments(for eventID: EventID) async throws -> [CommentDTO] {
        if let commentsError { throw commentsError }
        return commentsResult.filter { $0.eventID == eventID }
    }

    public func addComment(body: String, for userID: UserID, eventID: EventID) async throws -> CommentDTO {
        if let addError { throw addError }
        addedBodies.append(body)
        let dto = CommentDTO(
            id: UUID().uuidString,
            userID: userID,
            eventID: eventID,
            body: body,
            isApproved: true,
            isFlagged: false,
            createdAt: Date(),
            updatedAt: Date(),
            authorDisplayName: "Fake User",
            authorAvatarURL: nil
        )
        commentsResult.insert(dto, at: 0)
        return dto
    }
}
