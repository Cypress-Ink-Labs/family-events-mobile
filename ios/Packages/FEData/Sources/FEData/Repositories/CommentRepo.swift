import Foundation
import FECore

public protocol CommentRepo: Sendable {
    func comments(for eventID: EventID) async throws -> [CommentDTO]
    func addComment(body: String, for userID: UserID, eventID: EventID) async throws -> CommentDTO
}
