import Foundation
import FECore
import FEData

public final class FakeNotificationPreferencesRepo: NotificationPreferencesRepo, @unchecked Sendable {
    public var fetchResult: Result<NotificationPreferences, Error> = .success(.defaults)
    public var upsertResult: Result<NotificationPreferences, Error>?
    private(set) public var lastFetchedUserID: UserID?
    private(set) public var lastUpsertedUserID: UserID?
    private(set) public var lastUpsertedPreferences: NotificationPreferences?
    private(set) public var fetchCallCount: Int = 0
    private(set) public var upsertCallCount: Int = 0

    public init() {}

    public func fetch(userID: UserID) async throws -> NotificationPreferences {
        lastFetchedUserID = userID
        fetchCallCount += 1
        return try fetchResult.get()
    }

    public func upsert(_ preferences: NotificationPreferences, for userID: UserID) async throws -> NotificationPreferences {
        lastUpsertedUserID = userID
        lastUpsertedPreferences = preferences
        upsertCallCount += 1
        if let upsertResult {
            return try upsertResult.get()
        }
        fetchResult = .success(preferences)
        return preferences
    }
}
