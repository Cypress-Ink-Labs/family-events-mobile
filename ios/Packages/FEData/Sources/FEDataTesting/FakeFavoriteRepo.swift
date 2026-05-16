import Foundation
import FECore
import FEData

public final class FakeFavoriteRepo: FavoriteRepo, @unchecked Sendable {
    public var favoritesResult: Result<[FavoriteDTO], Error> = .success([])
    public var favoriteError: Error?
    public var unfavoriteError: Error?
    private(set) public var favoritedEventIDs: [EventID] = []
    private(set) public var unfavoritedEventIDs: [EventID] = []
    private(set) public var lastUserID: UserID?

    public init() {}

    public func favorites(for userID: UserID) async throws -> [FavoriteDTO] {
        lastUserID = userID
        return try favoritesResult.get()
    }

    public func favorite(eventID: EventID, for userID: UserID) async throws {
        if let err = favoriteError { throw err }
        favoritedEventIDs.append(eventID)
    }

    public func unfavorite(eventID: EventID, for userID: UserID) async throws {
        if let err = unfavoriteError { throw err }
        unfavoritedEventIDs.append(eventID)
    }

    public func observeFavorites(for userID: UserID) -> AsyncStream<FavoriteChange> {
        AsyncStream { _ in /* no-op for tests */ }
    }
}
