import XCTest
@testable import FEData
import FECore

final class SupabaseClientTests: XCTestCase {
    func testInitFromEnvConfig() {
        let config = EnvConfig(
            supabaseURL: URL(string: "https://example.supabase.co")!,
            supabaseAnonKey: "anon"
        )
        let client = FamilyEventsSupabase(config: config)
        XCTAssertEqual(client.config.supabaseURL.absoluteString, "https://example.supabase.co")
    }
}
