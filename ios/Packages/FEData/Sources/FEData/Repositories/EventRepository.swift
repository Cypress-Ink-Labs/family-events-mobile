import Foundation
import FECore
import Supabase

public protocol EventRepository: Sendable {
    func fetch(ids: [EventID], for userID: UserID) async throws -> [EventDTO]
}

/// Concrete impl chaining `.select("...")` after `.rpc("events_enriched", ...)` per D13.
/// supabase-swift 2.20.0's PostgrestFilterBuilder inherits select from PostgrestTransformBuilder.
public final class SupabaseEventRepository: EventRepository, @unchecked Sendable {
    private let supabase: FamilyEventsSupabase
    public init(supabase: FamilyEventsSupabase) { self.supabase = supabase }

    private static let eventColumns = "id,title,description,start_datetime,end_datetime,timezone,venue_name,address,city_id,latitude,longitude,age_min,age_max,price,is_free,source_url,source_name,source_id,images,status,ai_confidence,ai_tag_provider,is_featured,view_count,created_at,updated_at,avg_rating,rating_count,tags,is_favorited"

    public func fetch(ids: [EventID], for userID: UserID) async throws -> [EventDTO] {
        struct Params: Encodable {
            let p_event_ids: [String]
            let p_user_id: String
        }
        let params = Params(p_event_ids: ids.map(\.rawValue), p_user_id: userID.rawValue)
        let builder = try supabase.client.rpc("events_enriched", params: params)
        let response: PostgrestResponse<[EventDTO]> = try await builder
            .select(Self.eventColumns)
            .execute()
        return response.value
    }
}
