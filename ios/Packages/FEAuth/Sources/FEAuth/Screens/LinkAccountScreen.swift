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
            Text("Sign in to your account").font(.dsTitle2xl).frame(maxWidth: .infinity, alignment: .leading)
            Text("An account already exists for **\(viewModel.email)**. Sign in with your password to continue. You can connect Apple sign-in later in Profile settings.")
                .font(.dsBody)
                .foregroundStyle(Color.dsTextMuted)
                .frame(maxWidth: .infinity, alignment: .leading)

            SecureField("Password", text: $viewModel.password)
                .textContentType(.password)
                .padding().background(Color.dsSurfaceRaised).clipShape(.rect(cornerRadius: 8))

            if let err = viewModel.errorMessage {
                Text(err).foregroundStyle(.red).font(.dsCaptionXs)
            }

            Button {
                Task { await viewModel.submit() }
            } label: {
                if viewModel.isSubmitting { ProgressView() }
                else { Text("Sign in").frame(maxWidth: .infinity) }
            }
            .buttonStyle(.borderedProminent).controlSize(.large)
            .disabled(viewModel.isSubmitting)

            Button("Cancel", action: onCancel).buttonStyle(.plain)
            Spacer()
        }
        .padding()
        .navigationTitle("Sign in to your account")
    }
}
