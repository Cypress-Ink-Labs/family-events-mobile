import SwiftUI
import FECore
import FEDesignSystem

public struct SignUpScreen: View {
    @Bindable var viewModel: SignUpViewModel
    let onBackToSignIn: () -> Void

    public init(viewModel: SignUpViewModel, onBackToSignIn: @escaping () -> Void) {
        self.viewModel = viewModel
        self.onBackToSignIn = onBackToSignIn
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Create your account").font(.dsTitle2xl).frame(maxWidth: .infinity, alignment: .leading)

                TextField("Email", text: $viewModel.email)
                    #if os(iOS)
                    .textInputAutocapitalization(.never)
                    .keyboardType(.emailAddress)
                    #endif
                    .autocorrectionDisabled()
                    .textContentType(.emailAddress)
                    .padding().background(Color.dsSurfaceRaised).clipShape(.rect(cornerRadius: 8))

                SecureField("Password (at least 8 characters)", text: $viewModel.password)
                    .textContentType(.newPassword)
                    .padding().background(Color.dsSurfaceRaised).clipShape(.rect(cornerRadius: 8))

                if viewModel.pendingConfirmation {
                    Text("Check your email for a confirmation link. Once confirmed, sign in.")
                        .font(.dsBody)
                        .foregroundStyle(Color.dsTextMuted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button("Back to sign in", action: onBackToSignIn).buttonStyle(.borderedProminent)
                } else if let err = viewModel.errorMessage {
                    Text(err).foregroundStyle(.red).font(.dsCaptionXs)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                if !viewModel.pendingConfirmation {
                    Button {
                        Task { await viewModel.submit() }
                    } label: {
                        if viewModel.isSubmitting { ProgressView() }
                        else { Text("Create account").frame(maxWidth: .infinity) }
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .disabled(viewModel.isSubmitting)

                    Button("Already have an account? Sign in", action: onBackToSignIn).buttonStyle(.plain)
                }
            }
            .padding()
        }
        .navigationTitle("Sign up")
    }
}
