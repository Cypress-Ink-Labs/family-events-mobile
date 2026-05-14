import Foundation

enum ConsumerAPIPath: Equatable {
    case events
    case eventDetail(id: String)
    case favorites
    case profile

    var value: String {
        switch self {
        case .events:
            return "/api/v1/events"
        case .eventDetail(let id):
            return "/api/v1/events/\(id)"
        case .favorites:
            return "/api/v1/favorites"
        case .profile:
            return "/api/v1/profile"
        }
    }
}
