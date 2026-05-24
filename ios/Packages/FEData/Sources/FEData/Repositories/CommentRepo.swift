import Foundation
import FECore

public protocol CommentRepo: Sendable {
    func comments(for eventID: EventID) async throws -> [CommentDTO]
    func addComment(body: String, for userID: UserID, eventID: EventID) async throws -> CommentDTO
    /// Polling-backed change stream for a single event's comment thread.
    /// Mirrors SupabaseFavoriteRepo's poll-and-diff fallback because
    /// supabase-swift's realtime closures can't link under xcodegen.
    func observeComments(for eventID: EventID) -> AsyncStream<CommentChange>
}

public extension CommentRepo {
    /// Default no-op stream so existing test doubles don't have to implement
    /// observeComments. Real impls (SupabaseCommentRepo) override it.
    /// Finishes immediately so consumers awaiting completion (or
    /// `for await` loops without explicit cancellation) don't hang.
    func observeComments(for eventID: EventID) -> AsyncStream<CommentChange> {
        AsyncStream { continuation in
            continuation.finish()
        }
    }
}
