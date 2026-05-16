import Foundation
import SwiftData

@Model
public final class CachedExploreEvent {
    @Attribute(.unique) public var compositeKey: String  // "<filterSig>::<eventID>"
    public var filterSignature: String
    public var eventID: String
    public var rank: Int
    public var pageIndex: Int
    public var lastSyncedAt: Date

    public init(filterSignature: String, eventID: String, rank: Int, pageIndex: Int, lastSyncedAt: Date) {
        self.compositeKey = "\(filterSignature)::\(eventID)"
        self.filterSignature = filterSignature
        self.eventID = eventID
        self.rank = rank
        self.pageIndex = pageIndex
        self.lastSyncedAt = lastSyncedAt
    }
}
