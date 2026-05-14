import XCTest
import FECore
import FEData
@testable import FEAuth

final class SupabaseAuthServiceTests: XCTestCase {
    /// Compile-only smoke test — we don't have a credential to actually sign in
    /// against a real backend during unit tests. End-to-end coverage lives in
    /// FamilyEventsUITests (deferred to M2 integration step).
    func testCanInstantiateAgainstFamilyEventsSupabase() throws {
        let config = EnvConfig(
            supabaseURL: URL(string: "https://example.supabase.co")!,
            supabaseAnonKey: "anon"
        )
        let supabase = FamilyEventsSupabase(config: config)
        let service: any AuthService = SupabaseAuthService(supabase: supabase)
        XCTAssertNotNil(service)
    }
}
