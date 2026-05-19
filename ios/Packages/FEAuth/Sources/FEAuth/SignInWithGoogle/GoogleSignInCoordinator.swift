import Foundation
#if canImport(UIKit)
import UIKit
import GoogleSignIn
#endif
import FECore

// Debug-only logger so we don't leak OAuth instrumentation in Release builds.
#if DEBUG
@inline(__always) private func authDebugLog(_ message: @autoclosure () -> String) { print(message()) }
#else
@inline(__always) private func authDebugLog(_ message: @autoclosure () -> String) {}
#endif

public struct GoogleSignInResult: Sendable, Equatable {
    public let idToken: String
    public let rawNonce: String
    public let email: String?
    public init(idToken: String, rawNonce: String, email: String?) {
        self.idToken = idToken
        self.rawNonce = rawNonce
        self.email = email
    }
}

public enum GoogleSignInCoordinator {
    #if canImport(UIKit)
    /// Configures the shared GIDSignIn instance. Must be called once at app startup
    /// before any presentSignIn() call. Safe to call multiple times — last config wins.
    @MainActor
    public static func configure(iosClientID: String) {
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: iosClientID)
        authDebugLog("[GoogleSignIn] configured clientID=\(iosClientID.prefix(20))…")
    }

    /// Handles the redirect URL returned via SceneDelegate / openURL. Returns true
    /// when GoogleSignIn consumed the URL.
    @MainActor
    @discardableResult
    public static func handle(url: URL) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    @MainActor
    public static func presentSignIn(from presenter: UIViewController) async throws -> GoogleSignInResult {
        // Generate a per-request raw nonce, hash it, hand the hash to Google so
        // it gets embedded in the id_token. Supabase verifies the raw nonce
        // matches the hashed nonce in the token — replay protection without
        // relying on Supabase's "Skip nonce checks" escape hatch.
        let rawNonce = AppleSignInCoordinator.generateNonce()
        let hashedNonce = AppleSignInCoordinator.sha256(rawNonce)
        authDebugLog("[GoogleSignIn] presentSignIn start — config=\(GIDSignIn.sharedInstance.configuration?.clientID ?? "nil")")
        do {
            let result = try await GIDSignIn.sharedInstance.signIn(
                withPresenting: presenter,
                hint: nil,
                additionalScopes: nil,
                nonce: hashedNonce
            )
            guard let idToken = result.user.idToken?.tokenString else {
                authDebugLog("[GoogleSignIn] missing idToken on result")
                throw AppError.googleSignInFailed(NSError(domain: "GoogleSignIn", code: -1))
            }
            authDebugLog("[GoogleSignIn] success email=\(result.user.profile?.email ?? "nil") tokenLen=\(idToken.count)")
            return GoogleSignInResult(idToken: idToken, rawNonce: rawNonce, email: result.user.profile?.email)
        } catch let error as NSError where error.code == GIDSignInError.canceled.rawValue && error.domain == kGIDSignInErrorDomain {
            authDebugLog("[GoogleSignIn] cancelled by user")
            throw AppError.googleSignInCancelled
        } catch {
            authDebugLog("[GoogleSignIn] error: \(error)")
            throw AppError.googleSignInFailed(error)
        }
    }
    #endif
}
