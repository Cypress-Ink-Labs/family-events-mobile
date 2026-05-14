import SwiftUI
import FECore
import FEDesignSystem

public struct ResetPasswordScreen: View {
    @Bindable var viewModel: ResetPasswordViewModel
    let onDone: () -> Void

    public init(viewModel: ResetPasswordViewModel, onDone: @escaping () -> Void) {
        self.viewModel = viewModel
        self.onDone = onDone
    }

    public var body: some View {
        VStack(spacing: 24) {
            Text("Choose a new password").appTypography(.titleLarge).frame(maxWidth: .infinity, alignment: .leading)

            SecureField("New password (at least 8 characters)", text: $viewModel.newPassword)
                .textContentType(.newPassword)
                .padding().background(Color.appSecondaryBackground).cornerRadius(8)

            if viewModel.didReset {
                Text("Password updated. You're signed in.").appTypography(.body)
                Button("Continue", action: onDone).buttonStyle(.borderedProminent)
            } else {
                if let err = viewModel.errorMessage {
                    Text(err).foregroundStyle(.red).appTypography(.caption)
                }
                Button {
                    Task { await viewModel.submit() }
                } label: {
                    if viewModel.isSubmitting { ProgressView() }
                    else { Text("Update password").frame(maxWidth: .infinity) }
                }
                .buttonStyle(.borderedProminent).controlSize(.large)
                .disabled(viewModel.isSubmitting)
            }

            Spacer()
        }
        .padding()
        .navigationTitle("Reset password")
    }
}
