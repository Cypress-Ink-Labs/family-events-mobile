import SwiftUI
import FECore
import FEDesignSystem

public struct LinkAccountScreen: View {
    @Bindable var viewModel: LinkAccountViewModel
    let onCancel: () -> Void

    public init(viewModel: LinkAccountViewModel, onCancel: @escaping () -> Void) {
        self.viewModel = viewModel
        self.onCancel = onCancel
    }

    public var body: some View {
        VStack(spacing: 24) {
            Text("Connect your accounts").appTypography(.titleLarge).frame(maxWidth: .infinity, alignment: .leading)
            Text("An account already exists for **\(viewModel.email)**. Enter your password to sign in to that account and link Apple sign-in.")
                .appTypography(.body)
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)

            SecureField("Password", text: $viewModel.password)
                .textContentType(.password)
                .padding().background(Color.appSecondaryBackground).cornerRadius(8)

            if let err = viewModel.errorMessage {
                Text(err).foregroundStyle(.red).appTypography(.caption)
            }

            Button {
                Task { await viewModel.submit() }
            } label: {
                if viewModel.isSubmitting { ProgressView() }
                else { Text("Sign in and link").frame(maxWidth: .infinity) }
            }
            .buttonStyle(.borderedProminent).controlSize(.large)
            .disabled(viewModel.isSubmitting)

            Button("Cancel", action: onCancel).buttonStyle(.plain)
            Spacer()
        }
        .padding()
        .navigationTitle("Link accounts")
    }
}
