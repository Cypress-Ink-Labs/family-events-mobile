import Foundation
import FECore
import FEData

public final class FakeWeatherProviding: WeatherProviding, @unchecked Sendable {
    public var snapshotStub: WeatherSnapshot = WeatherSnapshot(
        temperatureCelsius: 20, precipitationChance: 0, conditionCode: "any"
    )
    public var errorStub: Error?
    private(set) public var callCount = 0

    public init() {}

    public func currentWeather(at coordinate: GeoCoordinate) async throws -> WeatherSnapshot {
        callCount += 1
        if let errorStub { throw errorStub }
        return snapshotStub
    }
}

// Backwards-compat alias for fakes named in earlier plan drafts.
public typealias FakeWeatherService = FakeWeatherProviding
