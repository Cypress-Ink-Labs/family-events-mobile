import Foundation
import FECore

/// Renamed from `WeatherService` to avoid a permanent type collision with
/// `WeatherKit.WeatherService` (D6).
public protocol WeatherProviding: Sendable {
    func currentWeather(at coordinate: GeoCoordinate) async throws -> WeatherSnapshot
}
