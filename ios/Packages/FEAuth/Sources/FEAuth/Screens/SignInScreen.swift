import SwiftUI
import FECore
import FEDesignSystem

public struct SignInScreen: View {
    @Bindable var viewModel: SignInViewModel
    let onForgotPassword: () -> Void
    let onSignUp: () -> Void
    let onAppleSignIn: () -> Void
    let onGoogleSignIn: () -> Void
    let googleSignInEnabled: Bool

    public init(
        viewModel: SignInViewModel,
        onForgotPassword: @escaping () -> Void,
        onSignUp: @escaping () -> Void,
        onAppleSignIn: @escaping () -> Void,
        onGoogleSignIn: @escaping () -> Void,
        googleSignInEnabled: Bool
    ) {
        self.viewModel = viewModel
        self.onForgotPassword = onForgotPassword
        self.onSignUp = onSignUp
        self.onAppleSignIn = onAppleSignIn
        self.onGoogleSignIn = onGoogleSignIn
        self.googleSignInEnabled = googleSignInEnabled
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Welcome back")
                    .appTypography(.titleLarge)
                    .frame(maxWidth: .infinity, alignment: .leading)

                AppleSignInButton(action: onAppleSignIn)
                    .frame(height: 48)

                if googleSignInEnabled {
                    GoogleSignInButton(action: onGoogleSignIn)
                }

                Divider()

                TextField("Email", text: $viewModel.email)
                    #if os(iOS)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.emailAddress)
                    #endif
                    .autocorrectionDisabled()
                    .textContentType(.emailAddress)
                    .padding().background(Color.appSecondaryBackground).cornerRadius(8)

                SecureField("Password", text: $viewModel.password)
                    .textContentType(.password)
                    .padding().background(Color.appSecondaryBackground).cornerRadius(8)

                if let err = viewModel.errorMessage {
                    Text(err).foregroundStyle(.red).appTypography(.caption)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                Button {
                    Task { await viewModel.submit() }
                } label: {
                    if viewModel.isSubmitting {
                        ProgressView()
                    } else {
                        Text("Sign in").frame(maxWidth: .infinity)
                    }
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.large)
                .disabled(viewModel.isSubmitting)

                HStack {
                    Button("Forgot password?", action: onForgotPassword).buttonStyle(.plain)
                    Spacer()
                    Button("Create account", action: onSignUp).buttonStyle(.plain)
                }
                .appTypography(.caption)
            }
            .padding()
        }
        .navigationTitle("Sign in")
    }
}
