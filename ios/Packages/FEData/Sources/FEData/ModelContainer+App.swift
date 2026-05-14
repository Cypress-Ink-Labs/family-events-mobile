import Foundation
import SwiftData

/// Centralised SwiftData container construction. M1 has no @Model types yet —
/// later milestones extend `allModelTypes` as CachedEvent, CachedFavorite,
/// etc. are added.
public enum AppModelContainer {
    /// Types registered with SwiftData. Empty in M1; extended in M3+.
    public static var allModelTypes: [any PersistentModel.Type] { [] }

    public static func makePersistent() throws -> ModelContainer {
        let schema = Schema(allModelTypes)
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        return try ModelContainer(for: schema, configurations: config)
    }

    public static func makeInMemory() throws -> ModelContainer {
        let schema = Schema(allModelTypes)
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
        return try ModelContainer(for: schema, configurations: config)
    }
}
