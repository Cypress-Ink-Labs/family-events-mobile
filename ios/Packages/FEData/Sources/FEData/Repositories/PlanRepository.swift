import Foundation
import FECore
import Supabase

public protocol PlanRepository: Sendable {
    func fetchPlan(input: PlanInput) async throws -> [PlanEventsRowDTO]
}

public struct PlanInput: Equatable, Sendable {
    public let userID: UserID
    public let date: String          // YYYY-MM-DD
    public let cityID: CityID?
    public let coordinate: GeoCoordinate?
    public let kidAge: Int?
    public let weatherFit: String    // "outdoor" | "indoor" | "any"
    public let limit: Int
    public let maxDays: Int

    public init(userID: UserID, date: String, cityID: CityID?, coordinate: GeoCoordinate?,
                kidAge: Int?, weatherFit: String, limit: Int = 3, maxDays: Int = 7) {
        self.userID = userID; self.date = date; self.cityID = cityID; self.coordinate = coordinate
        self.kidAge = kidAge; self.weatherFit = weatherFit; self.limit = limit; self.maxDays = maxDays
    }
}

public final class SupabasePlanRepository: PlanRepository, @unchecked Sendable {
    private let supabase: FamilyEventsSupabase
    public init(supabase: FamilyEventsSupabase) { self.supabase = supabase }

    public func fetchPlan(input: PlanInput) async throws -> [PlanEventsRowDTO] {
        struct Params: Encodable {
            let p_user_id: String
            let p_date: String
            let p_city_id: String?
            let p_lat: Double?
            let p_lng: Double?
            let p_kid_age: Int?
            let p_weather_fit: String
            let p_limit: Int
            let p_max_days: Int
        }
        let params = Params(
            p_user_id: input.userID.rawValue,
            p_date: input.date,
            p_city_id: input.cityID?.rawValue,
            p_lat: input.coordinate?.latitude,
            p_lng: input.coordinate?.longitude,
            p_kid_age: input.kidAge,
            p_weather_fit: input.weatherFit,
            p_limit: input.limit,
            p_max_days: input.maxDays
        )
        let builder = try supabase.client.rpc("plan_events_first_nonempty_window", params: params)
        let response: PostgrestResponse<[PlanEventsRowDTO]> = try await builder.execute()
        return response.value
    }
}
