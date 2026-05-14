import Foundation
import Observation

public enum SessionState: Equatable, Sendable {
    case signedOut
    case signedIn(userID: String)
}

@Observable
@MainActor
public final class SessionStore {
    public private(set) var state: SessionState = .signedOut

    public init() {}

    /// Test/transition seam — M2 replaces this with a real Supabase auth call.
    public func markSignedIn(userID: String) {
        state = .signedIn(userID: userID)
    }

    public func signOut() {
        state = .signedOut
    }
}
