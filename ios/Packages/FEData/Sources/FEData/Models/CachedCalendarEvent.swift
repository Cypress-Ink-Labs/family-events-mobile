import Foundation
import SwiftData

/// Local mirror of `user_calendar_events` rows. Keyed by composite
/// `<userID>::<eventID>` to make upsert-on-sync straightforward.
@Model
public final class CachedCalendarEvent {
    @Attribute(.unique) public var compositeKey: String
    public var userID: String
    public var eventID: String
    public var addedAt: Date
    public var notes: String?
    public var lastSyncedAt: Date

    public init(
        userID: String,
        eventID: String,
        addedAt: Date,
        notes: String?,
        lastSyncedAt: Date
    ) {
        self.compositeKey = "\(userID)::\(eventID)"
        self.userID = userID
        self.eventID = eventID
        self.addedAt = addedAt
        self.notes = notes
        self.lastSyncedAt = lastSyncedAt
    }
}
