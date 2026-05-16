import Foundation
import SwiftData

@Model
public final class CachedFavorite {
    @Attribute(.unique) public var compositeKey: String  // "<userID>::<eventID>"
    public var userID: String
    public var eventID: String
    public var createdAt: Date
    public var lastSyncedAt: Date

    public init(userID: String, eventID: String, createdAt: Date, lastSyncedAt: Date) {
        self.compositeKey = "\(userID)::\(eventID)"
        self.userID = userID
        self.eventID = eventID
        self.createdAt = createdAt
        self.lastSyncedAt = lastSyncedAt
    }
}
