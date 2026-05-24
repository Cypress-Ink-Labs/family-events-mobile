import Foundation
import SwiftData

/// Local mirror of `cities` rows so the city picker has a graceful offline
/// experience on cold start when the network is slow.
@Model
public final class CachedCity {
    @Attribute(.unique) public var id: String
    public var name: String
    public var stateCode: String?
    public var sortKey: String       // lowercased name for searchable sort
    public var lastSyncedAt: Date

    public init(
        id: String,
        name: String,
        stateCode: String?,
        lastSyncedAt: Date
    ) {
        self.id = id
        self.name = name
        self.stateCode = stateCode
        self.sortKey = name.lowercased()
        self.lastSyncedAt = lastSyncedAt
    }
}
