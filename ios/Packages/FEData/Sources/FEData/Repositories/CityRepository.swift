import Foundation
import FECore
import Supabase

public protocol CityRepository: Sendable {
    /// Returns the city name for the given ID, or nil if not found.
    func cityName(id: CityID) async throws -> String?
}

public final class SupabaseCityRepository: CityRepository, @unchecked Sendable {
    private let supabase: FamilyEventsSupabase
    public init(supabase: FamilyEventsSupabase) { self.supabase = supabase }

    public func cityName(id: CityID) async throws -> String? {
        struct Row: Decodable { let name: String }
        let response: PostgrestResponse<[Row]> = try await supabase.client
            .from("cities")
            .select("name")
            .eq("id", value: id.rawValue)
            .limit(1)
            .execute()
        return response.value.first?.name
    }
}
