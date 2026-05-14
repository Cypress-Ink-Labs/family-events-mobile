import SwiftUI
#if canImport(AuthenticationServices) && canImport(UIKit)
import AuthenticationServices

public struct AppleSignInButton: UIViewRepresentable {
    private let style: ASAuthorizationAppleIDButton.Style
    private let action: () -> Void

    public init(style: ASAuthorizationAppleIDButton.Style = .black, action: @escaping () -> Void) {
        self.style = style
        self.action = action
    }

    public func makeUIView(context: Context) -> ASAuthorizationAppleIDButton {
        let button = ASAuthorizationAppleIDButton(type: .signIn, style: style)
        button.addTarget(context.coordinator, action: #selector(Coordinator.didTap), for: .touchUpInside)
        return button
    }

    public func updateUIView(_ uiView: ASAuthorizationAppleIDButton, context: Context) {}

    public func makeCoordinator() -> Coordinator { Coordinator(action: action) }

    public final class Coordinator: NSObject {
        let action: () -> Void
        init(action: @escaping () -> Void) { self.action = action }
        @objc func didTap() { action() }
    }
}
#else
public struct AppleSignInButton: View {
    private let action: () -> Void
    public init(style: Int = 0, action: @escaping () -> Void) { self.action = action }
    public var body: some View {
        Button("Sign in with Apple", action: action)
    }
}
#endif
