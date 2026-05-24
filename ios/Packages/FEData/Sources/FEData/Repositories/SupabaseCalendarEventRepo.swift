import Foundation
import FECore
import Supabase

public final class SupabaseCalendarEventRepo: CalendarEventRepo, @unchecked Sendable {
    private let supabase: FamilyEventsSupabase
    private let pollInterval: Duration

    public init(supabase: FamilyEventsSupabase, pollInterval: Duration = .seconds(30)) {
        self.supabase = supabase
        self.pollInterval = pollInterval
    }

    public func calendarEvents(for userID: UserID) async throws -> [CalendarEventDTO] {
        let response: PostgrestResponse<[CalendarEventRow]> = try await supabase.client
            .from("user_calendar_events")
            .select("id,user_id,event_id,added_at,notes")
            .eq("user_id", value: userID.rawValue)
            .order("added_at", ascending: false)
            .execute()
        return response.value.map { $0.toDTO() }
    }

    public func add(eventID: EventID, notes: String?, for userID: UserID) async throws -> CalendarEventDTO {
        struct Payload: Encodable {
            let user_id: String
            let event_id: String
            let notes: String?
        }
        let response: PostgrestResponse<[CalendarEventRow]> = try await supabase.client
            .from("user_calendar_events")
            .insert(Payload(user_id: userID.rawValue, event_id: eventID.rawValue, notes: notes))
            .select("id,user_id,event_id,added_at,notes")
            .execute()
        guard let row = response.value.first else {
            throw AppError.notFound
        }
        return row.toDTO()
    }

    public func remove(eventID: EventID, for userID: UserID) async throws {
        _ = try await supabase.client
            .from("user_calendar_events")
            .delete()
            .eq("user_id", value: userID.rawValue)
            .eq("event_id", value: eventID.rawValue)
            .execute()
    }

    public func observeCalendarEvents(for userID: UserID) -> AsyncStream<CalendarEventChange> {
        let pollInterval = self.pollInterval
        return AsyncStream { continuation in
            let task = Task { [weak self] in
                guard let self else { return }
                var lastKnown: [String: CalendarEventDTO] = [:]
                if let initial = try? await self.calendarEvents(for: userID) {
                    lastKnown = Dictionary(uniqueKeysWithValues: initial.map { ($0.id, $0) })
                }
                while !Task.isCancelled {
                    try? await Task.sleep(for: pollInterval)
                    if Task.isCancelled { break }
                    guard let latest = try? await self.calendarEvents(for: userID) else { continue }
                    let latestByID = Dictionary(uniqueKeysWithValues: latest.map { ($0.id, $0) })
                    let latestIDs = Set(latestByID.keys)
                    let lastIDs = Set(lastKnown.keys)
                    for addedID in latestIDs.subtracting(lastIDs) {
                        if let dto = latestByID[addedID] {
                            continuation.yield(.added(dto))
                        }
                    }
                    for removedID in lastIDs.subtracting(latestIDs) {
                        if let dto = lastKnown[removedID] {
                            continuation.yield(.removed(eventID: dto.eventID))
                        }
                    }
                    lastKnown = latestByID
                }
            }
            continuation.onTermination = { _ in
                task.cancel()
            }
        }
    }
}

private struct CalendarEventRow: Decodable {
    let id: String
    let user_id: String
    let event_id: String
    let added_at: Date
    let notes: String?

    func toDTO() -> CalendarEventDTO {
        CalendarEventDTO(
            id: id,
            userID: UserID(user_id),
            eventID: EventID(event_id),
            addedAt: added_at,
            notes: notes
        )
    }
}
