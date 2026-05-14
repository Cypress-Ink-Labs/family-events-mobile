import Foundation
import FEAuth

public actor InMemoryKeychainStorage: KeychainStorage {
    private var storage: [KeychainKey: String] = [:]

    public init() {}

    public func string(for key: KeychainKey) async throws -> String? {
        storage[key]
    }

    public func setString(_ value: String, for key: KeychainKey) async throws {
        storage[key] = value
    }

    public func remove(_ key: KeychainKey) async throws {
        storage[key] = nil
    }

    public func removeAll() async throws {
        storage.removeAll()
    }
}
