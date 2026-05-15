import Foundation
import FECore
import FEData

public final class FakeProfileRepo: ProfileRepo, @unchecked Sendable {
    public var contextResult: Result<(cityID: CityID?, kidAge: Int?), Error> = .success((nil, nil))
    private(set) public var lastUserID: UserID?
    public init() {}
    public func currentContext(userID: UserID) async throws -> (cityID: CityID?, kidAge: Int?) {
        lastUserID = userID
        return try contextResult.get()
    }
}
