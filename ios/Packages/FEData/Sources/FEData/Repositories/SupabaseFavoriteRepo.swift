import Foundation
import FECore
import Supabase

/// Concrete implementation of FavoriteRepo backed by Supabase.
///
/// Realtime fallback: supabase-swift 2.20.0's Realtime APIs use `Helpers.AnyJSON`
/// and `Helpers.ObservationToken` in their closure signatures. `Helpers` is a
/// target inside supabase-swift, NOT a published product — so xcodegen-generated
/// xcodeproj frameworks cannot link those type-metadata symbols even though the
/// API is public at the Swift level. SPM via `swift test` resolves it transitively;
/// xcodebuild does not. Until supabase-swift exposes `Helpers` as a product, we
/// poll every 30s and diff against the previous snapshot to emit FavoriteChange
/// events. M7.1 will reinstate native realtime when the upstream packaging
/// supports it. Tracking note in CommitMessageBody.
public final class SupabaseFavoriteRepo: FavoriteRepo, @unchecked Sendable {
    private let supabase: FamilyEventsSupabase

    public init(supabase: FamilyEventsSupabase) {
        self.supabase = supabase
    }

    // MARK: – Fetch

    public func favorites(for userID: UserID) async throws -> [FavoriteDTO] {
        let response: PostgrestResponse<[FavoritesRow]> = try await supabase.client
            .from("favorites")
            .select("id,user_id,event_id,created_at")
            .eq("user_id", value: userID.rawValue)
            .execute()
        return response.value.map { $0.toDTO() }
    }

    // MARK: – Mutate

    public func favorite(eventID: EventID, for userID: UserID) async throws {
        struct Payload: Encodable { let user_id: String; let event_id: String }
        _ = try await supabase.client
            .from("favorites")
            .insert(Payload(user_id: userID.rawValue, event_id: eventID.rawValue))
            .execute()
    }

    public func unfavorite(eventID: EventID, for userID: UserID) async throws {
        _ = try await supabase.client
            .from("favorites")
            .delete()
            .eq("user_id", value: userID.rawValue)
            .eq("event_id", value: eventID.rawValue)
            .execute()
    }

    // MARK: – Realtime

    public func observeFavorites(for userID: UserID) -> AsyncStream<FavoriteChange> {
        AsyncStream { continuation in
            let task = Task { [weak self] in
                guard let self else { return }
                var lastKnown: [String: FavoriteDTO] = [:]
                // Seed from current server state on subscribe.
                if let initial = try? await self.favorites(for: userID) {
                    lastKnown = Dictionary(uniqueKeysWithValues: initial.map { ($0.id, $0) })
                }
                while !Task.isCancelled {
                    try? await Task.sleep(for: .seconds(30))
                    if Task.isCancelled { break }
                    guard let latest = try? await self.favorites(for: userID) else { continue }
                    let latestByID = Dictionary(uniqueKeysWithValues: latest.map { ($0.id, $0) })
                    let latestIDs = Set(latestByID.keys)
                    let lastIDs = Set(lastKnown.keys)
                    for addedID in latestIDs.subtracting(lastIDs) {
                        if let dto = latestByID[addedID] {
                            continuation.yield(.inserted(dto))
                        }
                    }
                    for removedID in lastIDs.subtracting(latestIDs) {
                        if let dto = lastKnown[removedID] {
                            continuation.yield(.deleted(eventID: dto.eventID))
                        }
                    }
                    lastKnown = latestByID
                }
            }
            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }
}

// MARK: – Helpers

private struct FavoritesRow: Decodable {
    let id: String
    let user_id: String
    let event_id: String
    let created_at: Date

    func toDTO() -> FavoriteDTO {
        FavoriteDTO(
            id: id,
            userID: UserID(user_id),
            eventID: EventID(event_id),
            createdAt: created_at
        )
    }
}
