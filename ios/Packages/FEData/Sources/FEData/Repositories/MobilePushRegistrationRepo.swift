import Foundation
import FECore
import Supabase

public enum MobilePushPlatform: String, Sendable {
    case ios
    case android
}

public protocol MobilePushRegistrationRepo: Sendable {
    func registerMobilePushToken(_ token: String, platform: MobilePushPlatform, for userID: UserID) async throws
}

public final class SupabaseMobilePushRegistrationRepo: MobilePushRegistrationRepo, Sendable {
    private let supabase: FamilyEventsSupabase

    public init(supabase: FamilyEventsSupabase) {
        self.supabase = supabase
    }

    public func registerMobilePushToken(_ token: String, platform: MobilePushPlatform, for userID: UserID) async throws {
        struct Params: Encodable {
            let p_platform: String
            let p_token: String
        }

        _ = try await supabase.client
            .rpc("register_push_subscription", params: Params(
                p_platform: platform.rawValue,
                p_token: token
            ))
            .execute()
    }
}
