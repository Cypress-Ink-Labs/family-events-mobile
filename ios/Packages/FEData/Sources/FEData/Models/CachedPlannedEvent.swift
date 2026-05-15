import Foundation
import SwiftData

@Model
public final class CachedPlannedEvent {
    @Attribute(.unique) public var eventID: String
    public var dayOffset: Int
    public var score: Double
    public var distanceKm: Double?
    public var rank: Int   // 0 = hero, 1+ = secondary
    public var lastSyncedAt: Date

    public init(eventID: String, dayOffset: Int, score: Double, distanceKm: Double?, lastSyncedAt: Date, rank: Int) {
        self.eventID = eventID
        self.dayOffset = dayOffset
        self.score = score
        self.distanceKm = distanceKm
        self.rank = rank
        self.lastSyncedAt = lastSyncedAt
    }
}
