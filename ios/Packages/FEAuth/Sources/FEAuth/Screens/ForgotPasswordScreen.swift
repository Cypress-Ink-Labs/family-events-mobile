import SwiftUI
import FECore
import FEDesignSystem

public struct ForgotPasswordScreen: View {
    @Bindable var viewModel: ForgotPasswordViewModel
    let onBack: () -> Void

    public init(viewModel: ForgotPasswordViewModel, onBack: @escaping () -> Void) {
        self.viewModel = viewModel
        self.onBack = onBack
    }

    public var body: some View {
        VStack(spacing: 24) {
            Text("Reset your password").font(.dsTitle2xl).frame(maxWidth: .infinity, alignment: .leading)
            Text("Enter your email and we'll send a reset link.")
                .font(.dsBody)
                .foregroundStyle(Color.dsTextMuted)
                .frame(maxWidth: .infinity, alignment: .leading)

            TextField("Email", text: $viewModel.email)
                #if os(iOS)
                .textInputAutocapitalization(.never)
                .keyboardType(.emailAddress)
                #endif
                .autocorrectionDisabled()
                .textContentType(.emailAddress)
                .padding().background(Color.dsSurfaceRaised).cornerRadius(8)

            if viewModel.emailSent {
                Text("Check your email for a reset link.").font(.dsBody)
                Button("Back to sign in", action: onBack).buttonStyle(.borderedProminent)
            } else {
                if let err = viewModel.errorMessage {
                    Text(err).foregroundStyle(.red).font(.dsCaptionXs)
                }
                Button {
                    Task { await viewModel.submit() }
                } label: {
                    if viewModel.isSubmitting { ProgressView() }
                    else { Text("Send reset link").frame(maxWidth: .infinity) }
                }
                .buttonStyle(.borderedProminent).controlSize(.large)
                .disabled(viewModel.isSubmitting)
            }

            Spacer()
        }
        .padding()
        .navigationTitle("Forgot password")
    }
}
