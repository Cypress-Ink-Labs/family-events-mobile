import SwiftUI
import FECore

public struct AuthRootView: View {
    private enum Screen: Hashable { case signUp, forgotPassword }

    @Environment(SessionStore.self) private var sessionStore
    private let authService: any AuthService
    private let googleSignInEnabled: Bool
    @State private var path: [Screen] = []

    public init(authService: any AuthService, googleSignInEnabled: Bool = false) {
        self.authService = authService
        self.googleSignInEnabled = googleSignInEnabled
    }

    public var body: some View {
        NavigationStack(path: $path) {
            SignInScreen(
                viewModel: SignInViewModel(authService: authService, sessionStore: sessionStore),
                onForgotPassword: { path.append(.forgotPassword) },
                onSignUp: { path.append(.signUp) },
                onAppleSignIn: { startAppleSignIn() },
                onGoogleSignIn: { startGoogleSignIn() },
                googleSignInEnabled: googleSignInEnabled
            )
            .navigationDestination(for: Screen.self) { screen in
                switch screen {
                case .signUp:
                    SignUpScreen(
                        viewModel: SignUpViewModel(authService: authService, sessionStore: sessionStore),
                        onBackToSignIn: { if !path.isEmpty { path.removeLast() } }
                    )
                case .forgotPassword:
                    ForgotPasswordScreen(
                        viewModel: ForgotPasswordViewModel(authService: authService),
                        onBack: { if !path.isEmpty { path.removeLast() } }
                    )
                }
            }
            .sheet(isPresented: linkRequiredBinding) {
                if case .linkRequired(let email, _, _) = sessionStore.state {
                    NavigationStack {
                        LinkAccountScreen(
                            viewModel: LinkAccountViewModel(email: email, authService: authService, sessionStore: sessionStore),
                            onCancel: { Task { await sessionStore.signOut() } }
                        )
                    }
                }
            }
        }
    }

    private var linkRequiredBinding: Binding<Bool> {
        Binding(
            get: {
                if case .linkRequired = sessionStore.state { return true }
                return false
            },
            set: { _ in /* dismissal handled inside the sheet */ }
        )
    }

    private func startAppleSignIn() {
        #if canImport(UIKit) && canImport(AuthenticationServices)
        Task { @MainActor in
            do {
                let scenes = UIApplication.shared.connectedScenes
                guard let windowScene = scenes.first as? UIWindowScene,
                      let anchor = windowScene.windows.first else { return }
                let result = try await AppleSignInCoordinator.presentSignIn(from: anchor)
                try await sessionStore.completeAppleSignIn(idToken: result.idToken, nonce: result.nonce, email: result.email)
            } catch AppError.appleSignInCancelled {
                return
            } catch {
                // Future: surface via a top-level alert binding.
            }
        }
        #endif
    }

    private func startGoogleSignIn() {
        #if canImport(UIKit)
        Task { @MainActor in
            do {
                let scenes = UIApplication.shared.connectedScenes
                guard let windowScene = scenes.first as? UIWindowScene,
                      let rootVC = windowScene.windows.first?.rootViewController else { return }
                // Walk to the topmost presented controller so the Google sheet stacks
                // on top of any modal that may already be showing.
                var presenter = rootVC
                while let next = presenter.presentedViewController { presenter = next }
                let result = try await GoogleSignInCoordinator.presentSignIn(from: presenter)
                try await sessionStore.completeGoogleSignIn(idToken: result.idToken, nonce: nil)
            } catch AppError.googleSignInCancelled {
                return
            } catch {
                // Future: surface via a top-level alert binding.
            }
        }
        #endif
    }
}
