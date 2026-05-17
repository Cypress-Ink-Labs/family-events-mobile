import Foundation
import FECore
import Supabase

public final class SupabaseRatingRepo: RatingRepo, @unchecked Sendable {
    private let supabase: FamilyEventsSupabase

    public init(supabase: FamilyEventsSupabase) {
        self.supabase = supabase
    }

    public func userRating(for userID: UserID, eventID: EventID) async throws -> RatingDTO? {
        let response: PostgrestResponse<[RatingsRow]> = try await supabase.client
            .from("ratings")
            .select("id,user_id,event_id,score,created_at")
            .eq("user_id", value: userID.rawValue)
            .eq("event_id", value: eventID.rawValue)
            .limit(1)
            .execute()
        return response.value.first?.toDTO()
    }

    public func upsertRating(score: Int, for userID: UserID, eventID: EventID) async throws -> RatingDTO {
        struct Payload: Encodable { let user_id: String; let event_id: String; let score: Int }
        let response: PostgrestResponse<[RatingsRow]> = try await supabase.client
            .from("ratings")
            .upsert(
                Payload(user_id: userID.rawValue, event_id: eventID.rawValue, score: score),
                onConflict: "user_id,event_id"
            )
            .select("id,user_id,event_id,score,created_at")
            .execute()
        guard let row = response.value.first else {
            throw AppError.notFound
        }
        return row.toDTO()
    }
}

private struct RatingsRow: Decodable {
    let id: String
    let user_id: String
    let event_id: String
    let score: Int
    let created_at: Date

    func toDTO() -> RatingDTO {
        RatingDTO(
            id: id,
            userID: UserID(user_id),
            eventID: EventID(event_id),
            score: score,
            createdAt: created_at
        )
    }
}
