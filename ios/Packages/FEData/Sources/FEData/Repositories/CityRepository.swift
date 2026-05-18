import Foundation
import FECore
import Supabase

public struct CitySummary: Identifiable, Equatable, Sendable {
    public let id: CityID
    public let name: String
    public let region: String?

    public init(id: CityID, name: String, region: String?) {
        self.id = id
        self.name = name
        self.region = region
    }
}

public protocol CityRepository: Sendable {
    func cities() async throws -> [CitySummary]
    func cityName(id: CityID) async throws -> String?
}

public final class SupabaseCityRepository: CityRepository, @unchecked Sendable {
    private let supabase: FamilyEventsSupabase
    public init(supabase: FamilyEventsSupabase) { self.supabase = supabase }

    public func cities() async throws -> [CitySummary] {
        struct Row: Decodable {
            let id: String
            let name: String
            let state: String?
        }
        let response: PostgrestResponse<[Row]> = try await supabase.client
            .from("cities")
            .select("id,name,state")
            .eq("is_active", value: true)
            .order("name", ascending: true)
            .execute()
        return response.value.map { CitySummary(id: CityID($0.id), name: $0.name, region: $0.state) }
    }

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
