import Foundation
import Supabase
import FECore

/// Thin wrapper around the Supabase SDK so callers depend on FEData, not the
/// SDK directly. Future milestones add typed methods (auth, query helpers,
/// realtime subscriptions) as extensions on this type.
public final class FamilyEventsSupabase: @unchecked Sendable {
    public let config: EnvConfig
    public let client: SupabaseClient

    public init(config: EnvConfig) {
        self.config = config
        self.client = SupabaseClient(
            supabaseURL: config.supabaseURL,
            supabaseKey: config.supabaseAnonKey
        )
    }
}
