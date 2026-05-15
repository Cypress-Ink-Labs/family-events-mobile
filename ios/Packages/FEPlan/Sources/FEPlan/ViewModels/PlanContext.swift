import Foundation
import FECore

public struct PlanContext: Equatable, Sendable {
    public let userID: UserID
    public let cityID: CityID?
    public let kidAge: Int?
    public init(userID: UserID, cityID: CityID? = nil, kidAge: Int? = nil) {
        self.userID = userID; self.cityID = cityID; self.kidAge = kidAge
    }
}
