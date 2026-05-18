import Foundation
import FECore
import FEData

public final class FakeCityRepository: CityRepository, @unchecked Sendable {
    public var nameResult: Result<String?, Error> = .success(nil)
    public var citiesResult: Result<[CitySummary], Error> = .success([])
    private(set) public var lastID: CityID?
    public init() {}
    public func cities() async throws -> [CitySummary] {
        return try citiesResult.get()
    }
    public func cityName(id: CityID) async throws -> String? {
        lastID = id
        return try nameResult.get()
    }
}
