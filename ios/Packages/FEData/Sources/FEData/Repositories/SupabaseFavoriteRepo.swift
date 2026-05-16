import Foundation
import FECore
import Supabase

/// Concrete implementation of FavoriteRepo backed by Supabase.
///
/// Realtime: native via supabase-swift 2.20.0 RealtimeV2 channel with
/// onPostgresChange(InsertAction/DeleteAction). Channel is created and
/// subscribed per call to observeFavorites; the caller cancels the returned
/// Task (or the AsyncStream continuation is dropped) to clean up.
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
            let channel = supabase.client.channel("favorites:\(userID.rawValue)")

            let insertToken = channel.onPostgresChange(
                InsertAction.self,
                schema: "public",
                table: "favorites",
                filter: "user_id=eq.\(userID.rawValue)"
            ) { action in
                guard
                    let id = action.record["id"]?.stringValue,
                    let eventIDStr = action.record["event_id"]?.stringValue,
                    let createdAtStr = action.record["created_at"]?.stringValue
                else { return }

                let date = ISO8601DateFormatter().date(from: createdAtStr) ?? Date()
                let dto = FavoriteDTO(
                    id: id,
                    userID: userID,
                    eventID: EventID(eventIDStr),
                    createdAt: date
                )
                continuation.yield(.inserted(dto))
            }

            let deleteToken = channel.onPostgresChange(
                DeleteAction.self,
                schema: "public",
                table: "favorites",
                filter: "user_id=eq.\(userID.rawValue)"
            ) { action in
                guard let eventIDStr = action.oldRecord["event_id"]?.stringValue else { return }
                continuation.yield(.deleted(eventID: EventID(eventIDStr)))
            }

            let task = Task {
                await channel.subscribe()
                // Keep tokens alive for the duration of the Task
                _ = insertToken
                _ = deleteToken
            }

            continuation.onTermination = { _ in
                task.cancel()
                Task { await self.supabase.client.removeChannel(channel) }
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
