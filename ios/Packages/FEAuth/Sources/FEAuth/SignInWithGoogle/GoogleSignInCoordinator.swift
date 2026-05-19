import Foundation
#if canImport(UIKit)
import UIKit
import GoogleSignIn
#endif
import FECore

public struct GoogleSignInResult: Sendable, Equatable {
    public let idToken: String
    public let email: String?
    public init(idToken: String, email: String?) {
        self.idToken = idToken
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
        do {
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
            guard let idToken = result.user.idToken?.tokenString else {
                throw AppError.googleSignInFailed(NSError(domain: "GoogleSignIn", code: -1))
            }
            return GoogleSignInResult(idToken: idToken, email: result.user.profile?.email)
        } catch let error as NSError where error.code == GIDSignInError.canceled.rawValue && error.domain == kGIDSignInErrorDomain {
            throw AppError.googleSignInCancelled
        } catch {
            throw AppError.googleSignInFailed(error)
        }
    }
    #endif
}
