import Foundation
import FECore
import Supabase

public protocol ProfileRepo: Sendable {
    /// Returns city preference + child age for the given user, or (nil, nil) if no row.
    /// Schema reference: profiles.city_preference_id, profiles.child_age (D14d).
    func currentContext(userID: UserID) async throws -> (cityID: CityID?, kidAge: Int?)
}

public final class SupabaseProfileRepo: ProfileRepo, @unchecked Sendable {
    private let supabase: FamilyEventsSupabase
    public init(supabase: FamilyEventsSupabase) { self.supabase = supabase }

    public func currentContext(userID: UserID) async throws -> (cityID: CityID?, kidAge: Int?) {
        struct Row: Decodable {
            let city_preference_id: String?
            let child_age: Int?
        }
        let response: PostgrestResponse<[Row]> = try await supabase.client
            .from("profiles")
            .select("city_preference_id,child_age")
            .eq("id", value: userID.rawValue)
            .limit(1)
            .execute()
        guard let first = response.value.first else { return (nil, nil) }
        return (first.city_preference_id.map(CityID.init), first.child_age)
    }
}
