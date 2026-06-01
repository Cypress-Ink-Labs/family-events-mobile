import Foundation
import FECore
import Supabase

public final class SupabaseCommentRepo: CommentRepo, Sendable {
    private let supabase: FamilyEventsSupabase
    private let pollInterval: Duration

    public init(supabase: FamilyEventsSupabase, pollInterval: Duration = .seconds(15)) {
        self.supabase = supabase
        self.pollInterval = pollInterval
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

    public func observeComments(for eventID: EventID) -> AsyncStream<CommentChange> {
        let pollInterval = self.pollInterval
        return AsyncStream { continuation in
            let task = Task { [weak self] in
                defer { continuation.finish() }
                guard let self else { return }
                var lastKnown: [String: CommentDTO] = [:]
                if let initial = try? await self.comments(for: eventID) {
                    lastKnown = Dictionary(uniqueKeysWithValues: initial.map { ($0.id, $0) })
                }
                while !Task.isCancelled {
                    try? await Task.sleep(for: pollInterval)
                    if Task.isCancelled { break }
                    guard let latest = try? await self.comments(for: eventID) else { continue }
                    let latestByID = Dictionary(uniqueKeysWithValues: latest.map { ($0.id, $0) })
                    let latestIDs = Set(latestByID.keys)
                    let lastIDs = Set(lastKnown.keys)
                    for addedID in latestIDs.subtracting(lastIDs) {
                        if let dto = latestByID[addedID] {
                            continuation.yield(.inserted(dto))
                        }
                    }
                    for removedID in lastIDs.subtracting(latestIDs) {
                        continuation.yield(.deleted(commentID: removedID))
                    }
                    for sharedID in latestIDs.intersection(lastIDs) {
                        if let prior = lastKnown[sharedID], let now = latestByID[sharedID],
                           prior != now {
                            continuation.yield(.updated(now))
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
