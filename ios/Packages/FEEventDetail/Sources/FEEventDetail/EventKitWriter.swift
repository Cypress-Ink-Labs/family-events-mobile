import Foundation
#if canImport(EventKit)
@preconcurrency import EventKit
#endif
import FEData

/// Maps `EventDTO` → `EKEvent` and writes to the user's default calendar.
/// Lives separate from the SwiftUI screen so EventKit access can be unit-tested
/// with a fake store and the screen stays presentation-only.
public struct EventKitWriter {
    public enum WriteError: Error, Equatable, Sendable {
        case accessDenied
        case noDefaultCalendar
        case underlying(String)
    }

    /// Default end-time when an event has no `endDatetime`. iOS event pickers
    /// expect a non-zero duration so the entry shows on the day strip.
    public static let defaultDurationSeconds: TimeInterval = 2 * 60 * 60

    #if canImport(EventKit)
    private let store: EKEventStore

    public init(store: EKEventStore = EKEventStore()) {
        self.store = store
    }

    public func addToCalendar(event: EventDTO) async throws {
        let granted = try await store.requestWriteOnlyAccessToEvents()
        guard granted else { throw WriteError.accessDenied }
        guard let calendar = store.defaultCalendarForNewEvents else {
            throw WriteError.noDefaultCalendar
        }
        let ek = EKEvent(eventStore: store)
        Self.populate(ek, from: event, calendar: calendar)
        do {
            try store.save(ek, span: .thisEvent, commit: true)
        } catch {
            throw WriteError.underlying(error.localizedDescription)
        }
    }

    static func populate(_ ek: EKEvent, from event: EventDTO, calendar: EKCalendar) {
        ek.calendar = calendar
        ek.title = event.title
        ek.startDate = event.startDatetime
        ek.endDate = event.endDatetime ?? event.startDatetime.addingTimeInterval(defaultDurationSeconds)
        ek.timeZone = TimeZone(identifier: event.timezone)
        ek.location = locationString(for: event)
        ek.notes = notesString(for: event)
    }
    #endif

    static func locationString(for event: EventDTO) -> String? {
        switch (event.venueName?.trimmedNonEmpty, event.address?.trimmedNonEmpty) {
        case let (venue?, address?): return "\(venue), \(address)"
        case let (venue?, nil): return venue
        case let (nil, address?): return address
        default: return nil
        }
    }

    static func notesString(for event: EventDTO) -> String? {
        var parts: [String] = []
        if let desc = event.description?.trimmedNonEmpty { parts.append(desc) }
        if let src = event.sourceURL?.trimmedNonEmpty { parts.append("Source: \(src)") }
        return parts.isEmpty ? nil : parts.joined(separator: "\n\n")
    }
}

private extension String {
    var trimmedNonEmpty: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}
