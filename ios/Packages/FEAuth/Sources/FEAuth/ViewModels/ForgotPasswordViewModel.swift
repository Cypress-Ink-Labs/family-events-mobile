import Foundation
import Observation
import FECore

@Observable
@MainActor
public final class ForgotPasswordViewModel {
    public var email: String = ""
    public private(set) var isSubmitting = false
    public private(set) var emailSent = false
    public private(set) var errorMessage: String?

    private let authService: any AuthService

    public init(authService: any AuthService) {
        self.authService = authService
    }

    public func submit() async {
        errorMessage = nil
        emailSent = false
        if let err = EmailPasswordValidators.emailError(email) { errorMessage = err; return }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            try await authService.sendPasswordResetEmail(email)
            emailSent = true
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }
}
