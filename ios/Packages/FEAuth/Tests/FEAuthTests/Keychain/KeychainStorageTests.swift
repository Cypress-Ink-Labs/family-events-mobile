import XCTest
@testable import FEAuth

final class KeychainStorageProtocolTests: XCTestCase {
    func testInMemoryStorageRoundTripsString() async throws {
        let storage: any KeychainStorage = InMemoryKeychainStorage()
        try await storage.setString("hello", for: .accessToken)
        let stored = try await storage.string(for: .accessToken)
        XCTAssertEqual(stored, "hello")
    }

    func testInMemoryStorageReturnsNilForMissingKey() async throws {
        let storage: any KeychainStorage = InMemoryKeychainStorage()
        let stored = try await storage.string(for: .refreshToken)
        XCTAssertNil(stored)
    }

    func testInMemoryStorageRemovesValues() async throws {
        let storage: any KeychainStorage = InMemoryKeychainStorage()
        try await storage.setString("x", for: .accessToken)
        try await storage.remove(.accessToken)
        let stored = try await storage.string(for: .accessToken)
        XCTAssertNil(stored)
    }

    func testInMemoryStorageRemoveAllClearsEverything() async throws {
        let storage: any KeychainStorage = InMemoryKeychainStorage()
        try await storage.setString("a", for: .accessToken)
        try await storage.setString("b", for: .refreshToken)
        try await storage.removeAll()
        let a = try await storage.string(for: .accessToken)
        let b = try await storage.string(for: .refreshToken)
        XCTAssertNil(a)
        XCTAssertNil(b)
    }
}

final class SecItemKeychainStorageTests: XCTestCase {
    /// Round-trip against the real keychain. Skipped when running under
    /// `swift test` from the command line (no keychain access there).
    func testSecItemRoundTrip() async throws {
        #if !canImport(UIKit)
        throw XCTSkip("SecItem keychain not available on this test toolchain.")
        #else
        let storage = SecItemKeychainStorage(service: "test.familyevents.auth")
        try? await storage.removeAll()
        try await storage.setString("token_xyz", for: .accessToken)
        let got = try await storage.string(for: .accessToken)
        XCTAssertEqual(got, "token_xyz")
        try await storage.removeAll()
        let cleared = try await storage.string(for: .accessToken)
        XCTAssertNil(cleared)
        #endif
    }
}
