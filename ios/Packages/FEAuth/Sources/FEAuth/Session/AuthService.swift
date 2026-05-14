import Foundation
import FECore

public enum IdentityProvider: String, Sendable, Equatable {
    case password
    case apple
}

public struct AuthSession: Equatable, Sendable {
    public let userID: UserID
    public let accessToken: String
    public let refreshToken: String
    public let email: String?
    public let identityProvider: IdentityProvider

    public init(
        userID: UserID,
        accessToken: String,
        refreshToken: String,
        email: String?,
        identityProvider: IdentityProvider
    ) {
        self.userID = userID
        self.accessToken = accessToken
        self.refreshToken = refreshToken
        self.email = email
        self.identityProvider = identityProvider
    }
}

public protocol AuthService: Sendable {
    func signIn(email: String, password: String) async throws -> AuthSession
    func signUp(email: String, password: String) async throws -> AuthSession
    func signInWithApple(idToken: String, nonce: String) async throws -> AuthSession
    func signOut() async throws
    func sendPasswordResetEmail(_ email: String) async throws
    func resetPassword(accessToken: String, newPassword: String) async throws -> AuthSession
    func deleteAccount() async throws
    /// Re-hydrate a session from stored refresh token on cold-start.
    func restoreSession(accessToken: String, refreshToken: String) async throws -> AuthSession
}
