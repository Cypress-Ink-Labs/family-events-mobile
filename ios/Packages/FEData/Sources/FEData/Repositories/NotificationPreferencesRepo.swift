import Foundation
import FECore
import Supabase

public struct NotificationPreferences: Equatable, Sendable {
    public var reminderEmail: Bool
    public var reminderPush: Bool
    public var changeEmail: Bool
    public var changePush: Bool
    public var digestEmail: Bool
    public var digestPush: Bool

    public static let defaults = NotificationPreferences(
        reminderEmail: true,
        reminderPush: true,
        changeEmail: true,
        changePush: true,
        digestEmail: true,
        digestPush: false
    )

    public init(
        reminderEmail: Bool,
        reminderPush: Bool,
        changeEmail: Bool,
        changePush: Bool,
        digestEmail: Bool,
        digestPush: Bool
    ) {
        self.reminderEmail = reminderEmail
        self.reminderPush = reminderPush
        self.changeEmail = changeEmail
        self.changePush = changePush
        self.digestEmail = digestEmail
        self.digestPush = digestPush
    }
}

public protocol NotificationPreferencesRepo: Sendable {
    func fetch(userID: UserID) async throws -> NotificationPreferences
    func upsert(_ preferences: NotificationPreferences, for userID: UserID) async throws -> NotificationPreferences
}

public final class SupabaseNotificationPreferencesRepo: NotificationPreferencesRepo, Sendable {
    private let supabase: FamilyEventsSupabase
    public init(supabase: FamilyEventsSupabase) { self.supabase = supabase }

    public func fetch(userID: UserID) async throws -> NotificationPreferences {
        let response: PostgrestResponse<[NotificationPreferencesRow]> = try await supabase.client
            .from("user_notification_preferences")
            .select(Self.selection)
            .eq("user_id", value: userID.rawValue)
            .limit(1)
            .execute()
        return response.value.first?.toPreferences() ?? .defaults
    }

    public func upsert(_ preferences: NotificationPreferences, for userID: UserID) async throws -> NotificationPreferences {
        struct Params: Encodable {
            let p_reminder_email: Bool
            let p_reminder_push: Bool
            let p_change_email: Bool
            let p_change_push: Bool
            let p_digest_email: Bool
            let p_digest_push: Bool
        }
        let params = Params(
            p_reminder_email: preferences.reminderEmail,
            p_reminder_push: preferences.reminderPush,
            p_change_email: preferences.changeEmail,
            p_change_push: preferences.changePush,
            p_digest_email: preferences.digestEmail,
            p_digest_push: preferences.digestPush
        )
        let response: PostgrestResponse<[NotificationPreferencesRow]> = try await supabase.client
            .rpc("upsert_notification_preferences", params: params)
            .execute()
        return response.value.first?.toPreferences() ?? preferences
    }

    private static let selection = "reminder_email,reminder_push,change_email,change_push,digest_email,digest_push"
}

private struct NotificationPreferencesRow: Decodable {
    let reminder_email: Bool
    let reminder_push: Bool
    let change_email: Bool
    let change_push: Bool
    let digest_email: Bool
    let digest_push: Bool

    func toPreferences() -> NotificationPreferences {
        NotificationPreferences(
            reminderEmail: reminder_email,
            reminderPush: reminder_push,
            changeEmail: change_email,
            changePush: change_push,
            digestEmail: digest_email,
            digestPush: digest_push
        )
    }
}
