import Foundation

#if !canImport(Security)
public typealias OSStatus = Int32
#endif

public enum AppError: Error, Sendable {
    case network(Error)
    case unauthorized
    case notFound
    case config(String)
    case invalidCredentials
    case emailAlreadyInUse
    case emailNotConfirmed
    case weakPassword(String)
    case appleSignInCancelled
    case appleSignInFailed(Error)
    case passwordResetEmailSent
    case keychain(OSStatus)
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
        case .invalidCredentials:
            return "Email or password is incorrect."
        case .emailAlreadyInUse:
            return "An account with that email already exists."
        case .emailNotConfirmed:
            return "Please confirm your email before signing in."
        case .weakPassword(let reason):
            return reason.isEmpty ? "Choose a stronger password." : reason
        case .appleSignInCancelled:
            return ""
        case .appleSignInFailed:
            return "Apple sign-in didn't complete. Please try again."
        case .passwordResetEmailSent:
            return "Check your email for a reset link."
        case .keychain:
            return "Couldn't securely store your session. Please try again."
        case .unknown:
            return "Something went wrong."
        }
    }
}
