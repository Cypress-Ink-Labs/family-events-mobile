import Foundation
import FECore

public protocol FavoriteRepo: Sendable {
    func favorites(for userID: UserID) async throws -> [FavoriteDTO]
    func favorite(eventID: EventID, for userID: UserID) async throws
    func unfavorite(eventID: EventID, for userID: UserID) async throws
    /// Long-running stream of incremental changes via Supabase realtime.
    /// Caller cancels the underlying Task to stop the subscription.
    func observeFavorites(for userID: UserID) -> AsyncStream<FavoriteChange>
}
