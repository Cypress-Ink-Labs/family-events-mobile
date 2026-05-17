import Foundation
import FECore
import Supabase

public final class SupabaseCommentRepo: CommentRepo, @unchecked Sendable {
    private let supabase: FamilyEventsSupabase

    public init(supabase: FamilyEventsSupabase) {
        self.supabase = supabase
    }

    public func comments(for eventID: EventID) async throws -> [CommentDTO] {
        let response: PostgrestResponse<[CommentsRow]> = try await supabase.client
            .from("comments")
            .select("id,user_id,event_id,body,is_approved,is_flagged,created_at,updated_at,user_profiles(display_name,avatar_url)")
            .eq("event_id", value: eventID.rawValue)
            .eq("is_approved", value: true)
            .order("created_at", ascending: false)
            .execute()
        return response.value.map { $0.toDTO() }
    }

    public func addComment(body: String, for userID: UserID, eventID: EventID) async throws -> CommentDTO {
        struct Payload: Encodable {
            let user_id: String
            let event_id: String
            let body: String
            let is_approved: Bool
            let is_flagged: Bool
        }
        let response: PostgrestResponse<[CommentsRow]> = try await supabase.client
            .from("comments")
            .insert(Payload(
                user_id: userID.rawValue,
                event_id: eventID.rawValue,
                body: body,
                is_approved: true,
                is_flagged: false
            ))
            .select("id,user_id,event_id,body,is_approved,is_flagged,created_at,updated_at,user_profiles(display_name,avatar_url)")
            .execute()
        guard let row = response.value.first else {
            throw AppError.notFound
        }
        return row.toDTO()
    }
}

private struct CommentsRow: Decodable {
    let id: String
    let user_id: String
    let event_id: String
    let body: String
    let is_approved: Bool
    let is_flagged: Bool
    let created_at: Date
    let updated_at: Date
    let user_profiles: AuthorProfile?

    struct AuthorProfile: Decodable {
        let display_name: String?
        let avatar_url: String?
    }

    func toDTO() -> CommentDTO {
        CommentDTO(
            id: id,
            userID: UserID(user_id),
            eventID: EventID(event_id),
            body: body,
            isApproved: is_approved,
            isFlagged: is_flagged,
            createdAt: created_at,
            updatedAt: updated_at,
            authorDisplayName: user_profiles?.display_name,
            authorAvatarURL: user_profiles?.avatar_url
        )
    }
}
