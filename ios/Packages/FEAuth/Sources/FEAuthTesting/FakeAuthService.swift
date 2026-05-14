import Foundation
import FECore
import FEAuth

public final class FakeAuthService: AuthService, @unchecked Sendable {
    public var signInResult: Result<AuthSession, Error> = .failure(AppError.unauthorized)
    public var signUpResult: Result<AuthSession, Error> = .failure(AppError.unauthorized)
    public var signInWithAppleResult: Result<AuthSession, Error> = .failure(AppError.appleSignInFailed(NSError(domain: "test", code: 0)))
    public var restoreResult: Result<AuthSession, Error> = .failure(AppError.unauthorized)
    public var signOutError: Error?
    public var sendResetError: Error?
    public var resetPasswordResult: Result<AuthSession, Error> = .failure(AppError.unauthorized)
    public var deleteAccountError: Error?

    public private(set) var signOutCallCount = 0
    public private(set) var deleteAccountCallCount = 0

    public init() {}

    public func signIn(email: String, password: String) async throws -> AuthSession {
        return try signInResult.get()
    }
    public func signUp(email: String, password: String) async throws -> AuthSession {
        return try signUpResult.get()
    }
    public func signInWithApple(idToken: String, nonce: String) async throws -> AuthSession {
        return try signInWithAppleResult.get()
    }
    public func signOut() async throws {
        signOutCallCount += 1
        if let signOutError { throw signOutError }
    }
    public func sendPasswordResetEmail(_ email: String) async throws {
        if let sendResetError { throw sendResetError }
    }
    public func resetPassword(accessToken: String, newPassword: String) async throws -> AuthSession {
        return try resetPasswordResult.get()
    }
    public func deleteAccount() async throws {
        deleteAccountCallCount += 1
        if let deleteAccountError { throw deleteAccountError }
    }
    public func restoreSession(accessToken: String, refreshToken: String) async throws -> AuthSession {
        return try restoreResult.get()
    }
}
