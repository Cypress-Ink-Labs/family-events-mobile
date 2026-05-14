import Foundation
import Observation
import FECore

@Observable
@MainActor
public final class ResetPasswordViewModel {
    public let token: String
    public var newPassword: String = ""
    public private(set) var isSubmitting = false
    public private(set) var didReset = false
    public private(set) var errorMessage: String?

    private let authService: any AuthService
    private let sessionStore: SessionStore

    public init(token: String, authService: any AuthService, sessionStore: SessionStore) {
        self.token = token
        self.authService = authService
        self.sessionStore = sessionStore
    }

    public func submit() async {
        errorMessage = nil
        if let err = EmailPasswordValidators.passwordError(newPassword) { errorMessage = err; return }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            let session = try await authService.resetPassword(accessToken: token, newPassword: newPassword)
            try await sessionStore.adopt(session)
            didReset = true
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }
}
