import Foundation
import FECore
#if canImport(Security)
import Security
#endif

public actor SecItemKeychainStorage: KeychainStorage {
    private let service: String

    public init(service: String) {
        self.service = service
    }

    public func string(for key: KeychainKey) async throws -> String? {
        #if canImport(Security)
        var query = baseQuery(for: key)
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne
        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        if status == errSecItemNotFound { return nil }
        if status != errSecSuccess { throw AppError.keychain(status) }
        guard let data = item as? Data,
              let s = String(data: data, encoding: .utf8) else { return nil }
        return s
        #else
        return nil
        #endif
    }

    public func setString(_ value: String, for key: KeychainKey) async throws {
        #if canImport(Security)
        let data = Data(value.utf8)
        let query = baseQuery(for: key)

        // Try update first (covers the value-exists case)
        let updateStatus = SecItemUpdate(
            query as CFDictionary,
            [kSecValueData as String: data] as CFDictionary
        )

        switch updateStatus {
        case errSecSuccess:
            return
        case errSecItemNotFound:
            var add = query
            add[kSecValueData as String] = data
            let addStatus = SecItemAdd(add as CFDictionary, nil)
            if addStatus != errSecSuccess { throw AppError.keychain(addStatus) }
        default:
            throw AppError.keychain(updateStatus)
        }
        #endif
    }

    public func remove(_ key: KeychainKey) async throws {
        #if canImport(Security)
        let status = SecItemDelete(baseQuery(for: key) as CFDictionary)
        if status != errSecSuccess && status != errSecItemNotFound {
            throw AppError.keychain(status)
        }
        #endif
    }

    public func removeAll() async throws {
        for key in KeychainKey.allCases {
            try await remove(key)
        }
    }

    #if canImport(Security)
    private func baseQuery(for key: KeychainKey) -> [String: Any] {
        return [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: key.rawValue,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]
    }
    #endif
}
