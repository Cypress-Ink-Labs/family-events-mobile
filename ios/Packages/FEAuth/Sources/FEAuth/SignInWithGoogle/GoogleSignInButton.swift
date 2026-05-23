import SwiftUI
import FEDesignSystem

public struct GoogleSignInButton: View {
    private let action: () -> Void

    public init(action: @escaping () -> Void) {
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: "g.circle.fill")
                    .foregroundStyle(.white)
                Text("Sign in with Google")
                    .font(.dsBody)
                    .foregroundStyle(.white)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(Color(red: 0.26, green: 0.52, blue: 0.96))
            .cornerRadius(8)
        }
        .buttonStyle(.plain)
    }
}
