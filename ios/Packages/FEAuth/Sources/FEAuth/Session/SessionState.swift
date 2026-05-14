import Foundation
import FECore

public enum SessionState: Equatable, Sendable {
    case hydrating
    case signedOut
    case signedIn(userID: UserID)
    case linkRequired(email: String, appleIdToken: String, nonce: String)
}
