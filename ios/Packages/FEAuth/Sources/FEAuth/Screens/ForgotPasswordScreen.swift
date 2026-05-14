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
            Text("Reset your password").appTypography(.titleLarge).frame(maxWidth: .infinity, alignment: .leading)
            Text("Enter your email and we'll send a reset link.")
                .appTypography(.body)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)

            TextField("Email", text: $viewModel.email)
                #if os(iOS)
                .textInputAutocapitalization(.never)
                .keyboardType(.emailAddress)
                #endif
                .autocorrectionDisabled()
                .textContentType(.emailAddress)
                .padding().background(Color.appSecondaryBackground).cornerRadius(8)

            if viewModel.emailSent {
                Text("Check your email for a reset link.").appTypography(.body)
                Button("Back to sign in", action: onBack).buttonStyle(.borderedProminent)
            } else {
                if let err = viewModel.errorMessage {
                    Text(err).foregroundStyle(.red).appTypography(.caption)
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
