import Foundation

public struct WeatherSnapshot: Equatable, Sendable {
    public let temperatureCelsius: Double
    public let precipitationChance: Double
    public let conditionCode: String

    public init(temperatureCelsius: Double, precipitationChance: Double, conditionCode: String) {
        self.temperatureCelsius = temperatureCelsius
        self.precipitationChance = precipitationChance
        self.conditionCode = conditionCode
    }

    /// Maps to the `weather_fit` enum the Supabase RPC expects: "outdoor", "indoor", or "any".
    public var weatherFit: String {
        if precipitationChance >= 0.4 { return "indoor" }
        if temperatureCelsius < 5 || temperatureCelsius > 35 { return "indoor" }
        if temperatureCelsius >= 16 && temperatureCelsius <= 30 && precipitationChance < 0.2 { return "outdoor" }
        return "any"
    }
}
