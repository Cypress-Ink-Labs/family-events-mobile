import Foundation

public enum ConsumerAPIPath: Equatable, Sendable {
    case events
    case eventDetail(id: EventID)
    case favorites
    case profile

    public var value: String {
        switch self {
        case .events:
            return "/api/v1/events"
        case .eventDetail(let id):
            return "/api/v1/events/\(id.rawValue)"
        case .favorites:
            return "/api/v1/favorites"
        case .profile:
            return "/api/v1/profile"
        }
    }
}
