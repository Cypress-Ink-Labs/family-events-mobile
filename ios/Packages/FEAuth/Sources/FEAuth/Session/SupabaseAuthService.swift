import Foundation
import Auth
import Supabase
import FECore
import FEData

public final class SupabaseAuthService: AuthService, Sendable {
    private let supabase: FamilyEventsSupabase

    public init(supabase: FamilyEventsSupabase) {
        self.supabase = supabase
    }

    public func signIn(email: String, password: String) async throws -> AuthSession {
        do {
            let session = try await supabase.client.auth.signIn(email: email, password: password)
            return Self.session(from: session, provider: .password)
        } catch {
            throw mapAuthError(error)
        }
    }

    public func signUp(email: String, password: String) async throws -> AuthSession {
        do {
            let response = try await supabase.client.auth.signUp(email: email, password: password)
            guard let session = response.session else { throw AppError.emailNotConfirmed }
            return Self.session(from: session, provider: .password)
        } catch let appError as AppError {
            throw appError
        } catch {
            throw mapAuthError(error)
        }
    }

    public func signInWithApple(idToken: String, nonce: String) async throws -> AuthSession {
        do {
            let session = try await supabase.client.auth.signInWithIdToken(
                credentials: OpenIDConnectCredentials(provider: .apple, idToken: idToken, nonce: nonce)
            )
            return Self.session(from: session, provider: .apple)
        } catch {
            throw AppError.appleSignInFailed(error)
        }
    }

    public func signOut() async throws {
        try await supabase.client.auth.signOut()
    }

    public func sendPasswordResetEmail(_ email: String) async throws {
        try await supabase.client.auth.resetPasswordForEmail(email)
    }

    public func resetPassword(accessToken: String, newPassword: String) async throws -> AuthSession {
        // Exchange the recovery token hash for a session, then update the password
        let response = try await supabase.client.auth.verifyOTP(tokenHash: accessToken, type: .recovery)
        guard let session = response.session else { throw AppError.unauthorized }
        _ = try await supabase.client.auth.update(user: UserAttributes(password: newPassword))
        return Self.session(from: session, provider: .password)
    }

    public func deleteAccount() async throws {
        _ = try await (try supabase.client.rpc("delete_my_account")).execute()
        try await signOut()
    }

    public func restoreSession(accessToken: String, refreshToken: String) async throws -> AuthSession {
        let session = try await supabase.client.auth.setSession(
            accessToken: accessToken,
            refreshToken: refreshToken
        )
        return Self.session(from: session, provider: .password)
    }

    private static func session(from supaSession: Session, provider: IdentityProvider) -> AuthSession {
        AuthSession(
            userID: UserID(supaSession.user.id.uuidString.lowercased()),
            accessToken: supaSession.accessToken,
            refreshToken: supaSession.refreshToken,
            email: supaSession.user.email,
            identityProvider: provider
        )
    }

    private func mapAuthError(_ error: Error) -> AppError {
        let message = (error as NSError).localizedDescription.lowercased()
        if message.contains("invalid login credentials") || message.contains("invalid_credentials") {
            return .invalidCredentials
        }
        if message.contains("already registered") || message.contains("user already exists") {
            return .emailAlreadyInUse
        }
        if message.contains("email not confirmed") {
            return .emailNotConfirmed
        }
        return .unknown(error)
    }
}
