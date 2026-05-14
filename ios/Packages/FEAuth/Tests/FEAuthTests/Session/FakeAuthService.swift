import Foundation
import FECore
@testable import FEAuth

final class FakeAuthService: AuthService, @unchecked Sendable {
    var signInResult: Result<AuthSession, Error> = .failure(AppError.unauthorized)
    var signUpResult: Result<AuthSession, Error> = .failure(AppError.unauthorized)
    var signInWithAppleResult: Result<AuthSession, Error> = .failure(AppError.appleSignInFailed(NSError(domain: "test", code: 0)))
    var restoreResult: Result<AuthSession, Error> = .failure(AppError.unauthorized)
    var signOutError: Error?
    var sendResetError: Error?
    var resetPasswordError: Error?
    var deleteAccountError: Error?

    private(set) var signOutCallCount = 0
    private(set) var deleteAccountCallCount = 0

    func signIn(email: String, password: String) async throws -> AuthSession {
        return try signInResult.get()
    }
    func signUp(email: String, password: String) async throws -> AuthSession {
        return try signUpResult.get()
    }
    func signInWithApple(idToken: String, nonce: String) async throws -> AuthSession {
        return try signInWithAppleResult.get()
    }
    func signOut() async throws {
        signOutCallCount += 1
        if let signOutError { throw signOutError }
    }
    func sendPasswordResetEmail(_ email: String) async throws {
        if let sendResetError { throw sendResetError }
    }
    func resetPassword(accessToken: String, newPassword: String) async throws {
        if let resetPasswordError { throw resetPasswordError }
    }
    func deleteAccount() async throws {
        deleteAccountCallCount += 1
        if let deleteAccountError { throw deleteAccountError }
    }
    func restoreSession(accessToken: String, refreshToken: String) async throws -> AuthSession {
        return try restoreResult.get()
    }
}
