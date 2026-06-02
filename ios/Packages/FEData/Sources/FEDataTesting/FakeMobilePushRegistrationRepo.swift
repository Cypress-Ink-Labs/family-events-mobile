import Foundation
import FECore
import FEData

public final class FakeMobilePushRegistrationRepo: MobilePushRegistrationRepo, @unchecked Sendable {
    public var registerResult: Result<Void, Error> = .success(())
    private(set) public var lastUserID: UserID?
    private(set) public var lastPlatform: MobilePushPlatform?
    private(set) public var lastToken: String?
    private(set) public var registerCallCount = 0

    public init() {}

    public func registerMobilePushToken(_ token: String, platform: MobilePushPlatform, for userID: UserID) async throws {
        lastUserID = userID
        lastPlatform = platform
        lastToken = token
        registerCallCount += 1
        try registerResult.get()
    }
}
