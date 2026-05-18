import XCTest
@testable import FECore

final class EnvConfigTests: XCTestCase {
    func testFailsWhenSupabaseURLMissing() {
        let bundle = StubBundle(values: ["SupabaseAnonKey": "anon"])
        XCTAssertThrowsError(try EnvConfig.load(from: bundle)) { error in
            guard case AppError.config(let key) = error else {
                XCTFail("expected config error, got \(error)")
                return
            }
            XCTAssertEqual(key, "SupabaseURL")
        }
    }

    func testFailsWhenSupabaseURLBlank() {
        let bundle = StubBundle(values: ["SupabaseURL": "  ", "SupabaseAnonKey": "anon"])
        XCTAssertThrowsError(try EnvConfig.load(from: bundle)) { error in
            guard case AppError.config(let key) = error else {
                XCTFail("expected config error, got \(error)")
                return
            }
            XCTAssertEqual(key, "SupabaseURL")
        }
    }

    func testFailsWhenAnonKeyMissing() {
        let bundle = StubBundle(values: ["SupabaseURL": "https://example.com"])
        XCTAssertThrowsError(try EnvConfig.load(from: bundle)) { error in
            guard case AppError.config(let key) = error else {
                XCTFail("expected config error, got \(error)")
                return
            }
            XCTAssertEqual(key, "SupabaseAnonKey")
        }
    }

    func testFailsWhenAnonKeyBlank() {
        let bundle = StubBundle(values: ["SupabaseURL": "https://example.com", "SupabaseAnonKey": "  "])
        XCTAssertThrowsError(try EnvConfig.load(from: bundle)) { error in
            guard case AppError.config(let key) = error else {
                XCTFail("expected config error, got \(error)")
                return
            }
            XCTAssertEqual(key, "SupabaseAnonKey")
        }
    }

    func testLoadsBothValues() throws {
        let bundle = StubBundle(values: [
            "SupabaseURL": "https://example.supabase.co",
            "SupabaseAnonKey": "anon_xxx",
        ])
        let config = try EnvConfig.load(from: bundle)
        XCTAssertEqual(config.supabaseURL.absoluteString, "https://example.supabase.co")
        XCTAssertEqual(config.supabaseAnonKey, "anon_xxx")
    }
}

private final class StubBundle: InfoPlistReader {
    let values: [String: Any]
    init(values: [String: Any]) { self.values = values }
    func object(forInfoDictionaryKey key: String) -> Any? { values[key] }
}
