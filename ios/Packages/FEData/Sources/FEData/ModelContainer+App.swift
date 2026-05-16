import Foundation
import SwiftData

public enum AppModelContainer {
    public static var allModelTypes: [any PersistentModel.Type] {
        [CachedEvent.self, CachedPlannedEvent.self, CachedExploreEvent.self, CachedFavorite.self]
    }

    public static func makePersistent() throws -> ModelContainer {
        let schema = Schema(allModelTypes)
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        return try ModelContainer(for: schema, configurations: config)
    }

    /// Overload for tests / explicit storage location. Used by the iron-rule regression test
    /// to simulate an "existing store from a previous milestone" without touching the host's
    /// default Application Support directory.
    public static func makePersistent(at url: URL) throws -> ModelContainer {
        let schema = Schema(allModelTypes)
        let config = ModelConfiguration(schema: schema, url: url)
        return try ModelContainer(for: schema, configurations: config)
    }

    public static func makeInMemory() throws -> ModelContainer {
        let schema = Schema(allModelTypes)
        let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
        return try ModelContainer(for: schema, configurations: config)
    }
}
