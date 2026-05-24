import Foundation
import FECore

public protocol CalendarEventRepo: Sendable {
    func calendarEvents(for userID: UserID) async throws -> [CalendarEventDTO]
    func add(eventID: EventID, notes: String?, for userID: UserID) async throws -> CalendarEventDTO
    func remove(eventID: EventID, for userID: UserID) async throws
    /// Polling-backed stream of changes. supabase-swift's realtime APIs
    /// can't link in xcodegen-generated projects today (Helpers target is
    /// not a published product), so this mirrors SupabaseFavoriteRepo's
    /// 30-second poll-and-diff fallback.
    func observeCalendarEvents(for userID: UserID) -> AsyncStream<CalendarEventChange>
}

public extension CalendarEventRepo {
    /// Default no-op stream so existing test doubles don't need to implement
    /// observeCalendarEvents. The Supabase impl overrides it.
    func observeCalendarEvents(for userID: UserID) -> AsyncStream<CalendarEventChange> {
        AsyncStream { _ in }
    }
}
