import Foundation

enum AppTab: String, CaseIterable, Identifiable, Hashable, Sendable {
    case plan
    case explore
    case saved

    var id: String { rawValue }

    var title: String {
        switch self {
        case .plan: return "Plan"
        case .explore: return "Explore"
        case .saved: return "Saved"
        }
    }

    var systemImage: String {
        switch self {
        case .plan: return "calendar.badge.clock"
        case .explore: return "sparkle.magnifyingglass"
        case .saved: return "bookmark.fill"
        }
    }
}
