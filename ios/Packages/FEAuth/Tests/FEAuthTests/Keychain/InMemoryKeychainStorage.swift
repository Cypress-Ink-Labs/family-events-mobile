import Foundation
@testable import FEAuth

actor InMemoryKeychainStorage: KeychainStorage {
    private var storage: [KeychainKey: String] = [:]

    func string(for key: KeychainKey) async throws -> String? {
        storage[key]
    }

    func setString(_ value: String, for key: KeychainKey) async throws {
        storage[key] = value
    }

    func remove(_ key: KeychainKey) async throws {
        storage[key] = nil
    }

    func removeAll() async throws {
        storage.removeAll()
    }
}
