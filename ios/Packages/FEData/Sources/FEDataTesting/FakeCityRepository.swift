import Foundation
import FECore
import FEData

public final class FakeCityRepository: CityRepository, @unchecked Sendable {
    public var nameResult: Result<String?, Error> = .success(nil)
    private(set) public var lastID: CityID?
    public init() {}
    public func cityName(id: CityID) async throws -> String? {
        lastID = id
        return try nameResult.get()
    }
}
