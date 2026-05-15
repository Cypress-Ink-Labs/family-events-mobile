import Foundation

/// Pure mapper from primitive WeatherKit values to our domain WeatherSnapshot.
/// Split out per D8 because WeatherKit's `CurrentWeather`/`HourWeather` types have
/// no public initializers in the iOS 17 SDK and cannot be constructed in unit tests.
/// The wire layer (WeatherKitService) extracts primitives from Apple's types and
/// hands them to this mapper, which is fully unit-testable.
public enum WeatherKitMapper {
    public static func map(
        temperatureCelsius: Double,
        hourlyPrecipitationChance: Double,
        conditionCode: String
    ) -> WeatherSnapshot {
        WeatherSnapshot(
            temperatureCelsius: temperatureCelsius,
            precipitationChance: clamp(hourlyPrecipitationChance, lower: 0, upper: 1),
            conditionCode: conditionCode.lowercased()
        )
    }

    private static func clamp<T: Comparable>(_ v: T, lower: T, upper: T) -> T {
        min(max(v, lower), upper)
    }
}
