import Foundation

/// Saved tab top-level filter. Matches web's My Events tabs.
public enum SavedFilter: String, CaseIterable, Identifiable, Sendable {
    case upcoming
    case past
    case all

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .upcoming: return "Upcoming"
        case .past: return "Past"
        case .all: return "All"
        }
    }

    /// Returns `true` when the event's start time matches this filter relative
    /// to `now`. "All" lets everything through; "Past" includes events whose
    /// start is before `now`; "Upcoming" is the inverse.
    public func includes(eventStart: Date, now: Date = Date()) -> Bool {
        switch self {
        case .all: return true
        case .past: return eventStart < now
        case .upcoming: return eventStart >= now
        }
    }
}
