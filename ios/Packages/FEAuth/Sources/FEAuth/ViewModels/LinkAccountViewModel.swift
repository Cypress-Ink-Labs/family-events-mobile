import Foundation
import Observation
import FECore

@Observable
@MainActor
public final class LinkAccountViewModel {
    public let email: String
    public var password: String = ""
    public private(set) var isSubmitting = false
    public private(set) var errorMessage: String?

    private let authService: any AuthService
    private let sessionStore: SessionStore

    public init(email: String, authService: any AuthService, sessionStore: SessionStore) {
        self.email = email
        self.authService = authService
        self.sessionStore = sessionStore
    }

    public func submit() async {
        errorMessage = nil
        if let err = EmailPasswordValidators.passwordError(password) { errorMessage = err; return }
        isSubmitting = true
        defer { isSubmitting = false }
        do {
            let session = try await authService.signIn(email: email, password: password)
            try await sessionStore.adopt(session)
        } catch let app as AppError {
            errorMessage = app.userMessage
        } catch {
            errorMessage = AppError.unknown(error).userMessage
        }
    }
}
