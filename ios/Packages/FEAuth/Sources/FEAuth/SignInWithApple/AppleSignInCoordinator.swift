import Foundation
import CryptoKit
#if canImport(AuthenticationServices) && canImport(UIKit)
import AuthenticationServices
import UIKit
#endif
import FECore

#if DEBUG
@inline(__always) private func authDebugLog(_ message: @autoclosure () -> String) { print(message()) }
#else
@inline(__always) private func authDebugLog(_ message: @autoclosure () -> String) {}
#endif

public struct AppleSignInResult: Sendable, Equatable {
    public let idToken: String
    public let nonce: String
    public let email: String?
    public init(idToken: String, nonce: String, email: String?) {
        self.idToken = idToken
        self.nonce = nonce
        self.email = email
    }
}

public enum AppleSignInCoordinator {
    /// Generates a cryptographically random nonce suitable for Apple's
    /// "rawNonce -> sha256 hex" requirement.
    public static func generateNonce(length: Int = 32) -> String {
        let charset = Array("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._")
        var result = ""
        var remaining = length
        while remaining > 0 {
            var randoms = [UInt8](repeating: 0, count: 16)
            _ = randoms.withUnsafeMutableBytes { buf in
                SecRandomCopyBytes(kSecRandomDefault, buf.count, buf.baseAddress!)
            }
            for byte in randoms {
                if remaining == 0 { break }
                let idx = Int(byte) % charset.count
                result.append(charset[idx])
                remaining -= 1
            }
        }
        return result
    }

    public static func sha256(_ value: String) -> String {
        let data = Data(value.utf8)
        let hash = SHA256.hash(data: data)
        return hash.map { String(format: "%02x", $0) }.joined()
    }

    #if canImport(AuthenticationServices) && canImport(UIKit)
    @MainActor
    public static func presentSignIn(from anchor: ASPresentationAnchor) async throws -> AppleSignInResult {
        authDebugLog("[AppleSignIn] presentSignIn start")
        let nonce = generateNonce()
        let request = ASAuthorizationAppleIDProvider().createRequest()
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)
        let controller = ASAuthorizationController(authorizationRequests: [request])
        let delegate = Delegate(rawNonce: nonce)
        let contextProvider = ContextProvider(anchor: anchor)
        controller.delegate = delegate
        controller.presentationContextProvider = contextProvider
        return try await withCheckedThrowingContinuation { continuation in
            delegate.continuation = continuation
            controller.performRequests()
            _ = contextProvider
        }
    }

    private final class Delegate: NSObject, ASAuthorizationControllerDelegate {
        let rawNonce: String
        var continuation: CheckedContinuation<AppleSignInResult, Error>?
        init(rawNonce: String) { self.rawNonce = rawNonce }
        deinit { continuation?.resume(throwing: CancellationError()) }

        func authorizationController(controller: ASAuthorizationController, didCompleteWithAuthorization authorization: ASAuthorization) {
            guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                  let tokenData = credential.identityToken,
                  let tokenString = String(data: tokenData, encoding: .utf8) else {
                authDebugLog("[AppleSignIn] missing idToken on credential")
                continuation?.resume(throwing: AppError.appleSignInFailed(NSError(domain: "AppleSignIn", code: -1)))
                return
            }
            authDebugLog("[AppleSignIn] success email=\(credential.email ?? "nil") tokenLen=\(tokenString.count)")
            continuation?.resume(returning: AppleSignInResult(idToken: tokenString, nonce: rawNonce, email: credential.email))
        }

        func authorizationController(controller: ASAuthorizationController, didCompleteWithError error: Error) {
            let nsError = error as NSError
            authDebugLog("[AppleSignIn] error domain=\(nsError.domain) code=\(nsError.code) desc=\(nsError.localizedDescription)")
            if let asError = error as? ASAuthorizationError, asError.code == .canceled {
                authDebugLog("[AppleSignIn] cancelled by user")
                continuation?.resume(throwing: AppError.appleSignInCancelled)
            } else {
                continuation?.resume(throwing: AppError.appleSignInFailed(error))
            }
        }
    }

    private final class ContextProvider: NSObject, ASAuthorizationControllerPresentationContextProviding {
        let anchor: ASPresentationAnchor
        init(anchor: ASPresentationAnchor) { self.anchor = anchor }
        func presentationAnchor(for controller: ASAuthorizationController) -> ASPresentationAnchor { anchor }
    }
    #endif
}
