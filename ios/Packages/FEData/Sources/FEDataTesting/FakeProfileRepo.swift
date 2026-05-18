import Foundation
import FECore
import FEData

public final class FakeProfileRepo: ProfileRepo, @unchecked Sendable {
    public var contextResult: Result<(cityID: CityID?, kidAge: Int?), Error> = .success((nil, nil))
    public var profileResult: Result<UserProfile?, Error> = .success(nil)
    public var updateResult: Result<UserProfile, Error>?
    private(set) public var lastUserID: UserID?
    private(set) public var lastUpdate: UserProfileUpdate?
    public init() {}
    public func profile(userID: UserID) async throws -> UserProfile? {
        lastUserID = userID
        return try profileResult.get()
    }
    public func updateProfile(_ update: UserProfileUpdate, for userID: UserID) async throws -> UserProfile {
        lastUserID = userID
        lastUpdate = update
        if let updateResult {
            return try updateResult.get()
        }
        let updated = UserProfile(
            id: userID,
            email: nil,
            displayName: update.displayName,
            avatarURL: nil,
            cityPreferenceID: update.cityPreferenceID,
            childName: update.childName,
            childAge: update.childAge
        )
        profileResult = .success(updated)
        contextResult = .success((updated.cityPreferenceID, updated.childAge))
        return updated
    }
    public func currentContext(userID: UserID) async throws -> (cityID: CityID?, kidAge: Int?) {
        lastUserID = userID
        return try contextResult.get()
    }
}
