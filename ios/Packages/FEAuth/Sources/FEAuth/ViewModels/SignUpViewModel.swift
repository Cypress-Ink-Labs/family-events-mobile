import Foundation
import Observation
import FECore

@Observable
@MainActor
public final class SignUpViewModel {
    public var email: String = ""
    public var password: String = ""
    public private(set) var isSubmitting = false
    public private(set) var errorMessage: String?
    public private(set) var pendingConfirmation = false

    private let authService: any AuthService
    private let sessionStore: SessionStore

    public init(authService: any AuthService, sessionStore: SessionStore) {
        self.authService = authService
        self.sessionStore = sessionStore
    }

    public func submit() async {
        errorMessage = nil
        pendingConfirmation = false
        if let err = EmailPasswordValidators.emailError(email) { errorMessage = err; return }
        if let err = EmailPasswordValidators.passwordError(password) { errorMessage = err; return }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            let session = try await authService.signUp(email: email, password: password)
            try await sessionStore.adopt(session)
        } catch AppError.emailNotConfirmed {
            pendingConfirmation = true
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }
}
