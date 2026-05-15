import Foundation
import FECore
#if canImport(WeatherKit) && canImport(CoreLocation)
import WeatherKit
import CoreLocation
#endif

#if canImport(WeatherKit) && canImport(CoreLocation)
public final class WeatherKitService: WeatherProviding, Sendable {
    public init() {}
    public func currentWeather(at coordinate: GeoCoordinate) async throws -> WeatherSnapshot {
        let location = CLLocation(latitude: coordinate.latitude, longitude: coordinate.longitude)
        let weather = try await WeatherKit.WeatherService.shared.weather(for: location)
        return WeatherKitMapper.map(
            temperatureCelsius: weather.currentWeather.temperature.converted(to: .celsius).value,
            hourlyPrecipitationChance: weather.hourlyForecast.first?.precipitationChance ?? 0,
            conditionCode: weather.currentWeather.condition.description
        )
    }
}
#endif
