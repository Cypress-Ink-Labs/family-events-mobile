import Foundation
import Observation
import FECore

@Observable
@MainActor
public final class SessionStore {
    public private(set) var state: SessionState
    private let authService: any AuthService
    private let storage: any KeychainStorage

    public init(authService: any AuthService, storage: any KeychainStorage) {
        self.authService = authService
        self.storage = storage
        self.state = .hydrating
        Task { await self.bootstrap() }
    }

    private func bootstrap() async {
        do {
            guard
                let access = try await storage.string(for: .accessToken),
                let refresh = try await storage.string(for: .refreshToken)
            else {
                state = .signedOut
                return
            }
            let session = try await authService.restoreSession(accessToken: access, refreshToken: refresh)
            try await persist(session)
            state = .signedIn(userID: session.userID)
        } catch {
            try? await storage.removeAll()
            state = .signedOut
        }
    }

    public func markLinkRequired(email: String, appleIdToken: String, nonce: String) {
        state = .linkRequired(email: email, appleIdToken: appleIdToken, nonce: nonce)
    }

    public func adopt(_ session: AuthSession) async throws {
        try await persist(session)
        state = .signedIn(userID: session.userID)
    }

    public func signOut() async {
        try? await authService.signOut()
        try? await storage.removeAll()
        state = .signedOut
    }

    private func persist(_ session: AuthSession) async throws {
        try await storage.setString(session.accessToken, for: .accessToken)
        try await storage.setString(session.refreshToken, for: .refreshToken)
        try await storage.setString(session.userID.rawValue, for: .userID)
    }
}

extension SessionStore {
    public func completeAppleSignIn(idToken: String, nonce: String, email: String?) async throws {
        do {
            let session = try await authService.signInWithApple(idToken: idToken, nonce: nonce)
            try await adopt(session)
        } catch AppError.emailAlreadyInUse {
            state = .linkRequired(email: email ?? "", appleIdToken: idToken, nonce: nonce)
        } catch AppError.appleSignInCancelled {
            return
        } catch {
            throw error
        }
    }
}
