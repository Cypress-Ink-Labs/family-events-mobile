import Foundation
import Supabase
import FECore

/// Thin wrapper around the Supabase SDK so callers depend on FEData, not the
/// SDK directly. Future milestones add typed methods (auth, query helpers,
/// realtime subscriptions) as extensions on this type.
public final class FamilyEventsSupabase: Sendable {
    public let config: EnvConfig
    public let client: SupabaseClient

    public init(config: EnvConfig) {
        self.config = config
        // emitLocalSessionAsInitialSession opts into the supabase-swift 3.x
        // behaviour now (see PR 822): the locally-stored session is always
        // emitted on startup regardless of expiry, so we can branch on
        // session.isExpired ourselves instead of relying on the SDK to gate it.
        // Without this flag we'd silently break on the next major upgrade.
        let options = SupabaseClientOptions(
            auth: SupabaseClientOptions.AuthOptions(emitLocalSessionAsInitialSession: true)
        )
        self.client = SupabaseClient(
            supabaseURL: config.supabaseURL,
            supabaseKey: config.supabaseAnonKey,
            options: options
        )
    }
}
