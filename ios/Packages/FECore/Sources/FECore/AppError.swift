import Foundation

public enum AppError: Error, Sendable {
    case network(Error)
    case unauthorized
    case notFound
    case config(String)
    case unknown(Error)

    public var userMessage: String {
        switch self {
        case .network:
            return "Network problem. Please try again."
        case .unauthorized:
            return "You're signed out. Please sign in again."
        case .notFound:
            return "We couldn't find that."
        case .config(let key):
            return "Configuration error: \(key) is missing."
        case .unknown:
            return "Something went wrong."
        }
    }
}
