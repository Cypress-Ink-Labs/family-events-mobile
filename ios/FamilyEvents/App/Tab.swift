import Foundation

enum AppTab: String, CaseIterable, Identifiable, Hashable, Sendable {
    case plan
    case explore
    case saved
    case admin

    var id: String { rawValue }

    var title: String {
        switch self {
        case .plan: return "Plan"
        case .explore: return "Explore"
        case .saved: return "Saved"
        case .admin: return "Admin"
        }
    }

    var systemImage: String {
        switch self {
        case .plan: return "calendar.badge.clock"
        case .explore: return "sparkle.magnifyingglass"
        case .saved: return "bookmark.fill"
        case .admin: return "person.badge.key"
        }
    }
}
