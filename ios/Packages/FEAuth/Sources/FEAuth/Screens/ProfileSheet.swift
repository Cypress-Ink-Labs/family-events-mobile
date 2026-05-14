import SwiftUI
import FECore
import FEDesignSystem

public struct ProfileSheet: View {
    @Environment(SessionStore.self) private var sessionStore
    @Environment(\.dismiss) private var dismiss
    private let authService: any AuthService
    @State private var showDeleteConfirmation = false

    public init(authService: any AuthService) {
        self.authService = authService
    }

    public var body: some View {
        NavigationStack {
            List {
                if case .signedIn(let uid) = sessionStore.state {
                    Section("Account") {
                        LabeledContent("User ID", value: uid.rawValue)
                    }
                }
                Section {
                    Button("Sign out") {
                        Task { await sessionStore.signOut(); dismiss() }
                    }
                }
                Section {
                    Button("Delete account", role: .destructive) {
                        showDeleteConfirmation = true
                    }
                } footer: {
                    Text("Deleting your account permanently removes your saved events, ratings, and comments. This cannot be undone.")
                }
            }
            .navigationTitle("Profile")
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Done") { dismiss() } } }
            .confirmationDialog(
                "Delete account?",
                isPresented: $showDeleteConfirmation,
                titleVisibility: .visible
            ) {
                Button("Delete forever", role: .destructive) {
                    Task {
                        try? await authService.deleteAccount()
                        await sessionStore.signOut()
                        dismiss()
                    }
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This will delete your account and all associated data. You can't recover it later.")
            }
        }
    }
}
