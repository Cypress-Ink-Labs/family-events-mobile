import Foundation

public protocol KeychainStorage: Sendable {
    func string(for key: KeychainKey) async throws -> String?
    func setString(_ value: String, for key: KeychainKey) async throws
    func remove(_ key: KeychainKey) async throws
    func removeAll() async throws
}
