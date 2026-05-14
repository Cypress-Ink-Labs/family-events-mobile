import Foundation

public enum AppTab: String, CaseIterable, Identifiable, Hashable, Sendable {
    case plan
    case explore
    case saved

    public var id: String { rawValue }

    public var title: String {
        switch self {
        case .plan: return "Plan"
        case .explore: return "Explore"
        case .saved: return "Saved"
        }
    }

    public var systemImage: String {
        switch self {
        case .plan: return "calendar.badge.clock"
        case .explore: return "sparkle.magnifyingglass"
        case .saved: return "bookmark.fill"
        }
    }
}
