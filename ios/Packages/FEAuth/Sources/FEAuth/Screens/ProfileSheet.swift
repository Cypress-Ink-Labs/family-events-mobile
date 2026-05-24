import SwiftUI
import FECore
import FEData
import FEDesignSystem

public struct ProfileSheet: View {
    @Environment(SessionStore.self) private var sessionStore
    @Environment(\.dismiss) private var dismiss
    @AppStorage("family-events-theme") private var appearanceRawValue = AppAppearancePreference.system.rawValue
    private let authService: any AuthService
    private let profileRepo: any ProfileRepo
    private let cityRepo: any CityRepository
    private let onProfileSaved: (UserProfile) -> Void
    @State private var showDeleteConfirmation = false
    @State private var viewModel: ProfileViewModel?

    public init(
        authService: any AuthService,
        profileRepo: any ProfileRepo,
        cityRepo: any CityRepository,
        onProfileSaved: @escaping (UserProfile) -> Void = { _ in }
    ) {
        self.authService = authService
        self.profileRepo = profileRepo
        self.cityRepo = cityRepo
        self.onProfileSaved = onProfileSaved
    }

    public var body: some View {
        NavigationStack {
            Group {
                switch sessionStore.state {
                case .signedIn:
                    if let viewModel {
                        ProfileForm(
                            viewModel: viewModel,
                            appearanceRawValue: $appearanceRawValue,
                            onSave: { saved in onProfileSaved(saved) },
                            onSignOut: {
                                Task {
                                    await sessionStore.signOut()
                                    dismiss()
                                }
                            },
                            onDelete: { showDeleteConfirmation = true }
                        )
                    } else {
                        ProgressView()
                            .controlSize(.large)
                    }
                case .hydrating:
                    ProgressView()
                        .controlSize(.large)
                case .signedOut, .linkRequired:
                    ContentUnavailableView("Signed out", systemImage: "person.crop.circle.badge.xmark")
                }
            }
            .navigationTitle("Profile")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .task(id: signedInUserID?.rawValue) {
                guard let userID = signedInUserID else {
                    viewModel = nil
                    return
                }
                viewModel = nil
                let model = ProfileViewModel(
                    userID: userID,
                    profileRepo: profileRepo,
                    cityRepo: cityRepo,
                    authService: authService
                )
                viewModel = model
                await model.load()
            }
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

    private var signedInUserID: UserID? {
        if case .signedIn(let userID) = sessionStore.state {
            return userID
        }
        return nil
    }
}

private struct ProfileForm: View {
    @Bindable var viewModel: ProfileViewModel
    @Binding var appearanceRawValue: String
    let onSave: (UserProfile) -> Void
    let onSignOut: () -> Void
    let onDelete: () -> Void

    var body: some View {
        List {
            accountSection
            personalInfoSection
            appearanceSection
            passwordSection
            destructiveSection
        }
        .disabled(viewModel.isLoading)
        .overlay {
            if viewModel.isLoading {
                ProgressView()
                    .controlSize(.large)
            }
        }
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button(viewModel.isSaving ? "Saving..." : "Save") {
                    Task {
                        if let saved = await viewModel.save() {
                            onSave(saved)
                        }
                    }
                }
                .disabled(viewModel.isSaving || !viewModel.hasUnsavedChanges)
            }
            ToolbarItem(placement: .cancellationAction) {
                Button("Revert") {
                    viewModel.discardChanges()
                }
                .disabled(!viewModel.hasUnsavedChanges)
            }
        }
    }

    private var accountSection: some View {
        Section("Account") {
            HStack(spacing: 12) {
                avatar
                VStack(alignment: .leading, spacing: 4) {
                    Text(viewModel.profile?.displayName?.isEmpty == false ? viewModel.profile?.displayName ?? "Family member" : "Family member")
                        .font(.headline)
                    if let email = viewModel.profile?.email, !email.isEmpty {
                        Text(email)
                            .font(.subheadline)
                            .foregroundStyle(Color.dsTextMuted)
                    }
                }
            }
            LabeledContent("User ID", value: viewModel.userID.rawValue)
                .font(.caption)
        }
    }

    private var avatar: some View {
        Group {
            if
                let rawURL = viewModel.profile?.avatarURL,
                let url = URL(string: rawURL)
            {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    default:
                        avatarFallback
                    }
                }
            } else {
                avatarFallback
            }
        }
        .frame(width: 52, height: 52)
        .clipShape(Circle())
    }

    private var avatarFallback: some View {
        Circle()
            .fill(Color.dsAccentPrimarySoft)
            .overlay {
                Text(initials)
                    .font(.headline)
                    .foregroundStyle(Color.dsAccentPrimary)
            }
    }

    private var initials: String {
        let value = viewModel.profile?.displayName ?? viewModel.profile?.email ?? "U"
        return String(value.prefix(1)).uppercased()
    }

    private var personalInfoSection: some View {
        Section("Personal Info") {
            TextField("Your Name", text: $viewModel.displayName)
                .textContentType(.name)
            TextField("Child's Name (optional)", text: $viewModel.childName)
                .textContentType(.givenName)
            TextField("Child's Age (optional)", text: $viewModel.childAgeText)
            Picker("Preferred City", selection: $viewModel.selectedCityID) {
                Text("None").tag(nil as CityID?)
                ForEach(viewModel.cities) { city in
                    Text(cityTitle(city)).tag(Optional(city.id))
                }
            }
            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.footnote)
                    .foregroundStyle(Color.dsError)
            }
        }
    }

    private var appearanceSection: some View {
        Section("Appearance") {
            ThemePickerCard(appearanceRawValue: $appearanceRawValue)
        }
    }

    private var passwordSection: some View {
        PasswordSection(viewModel: viewModel)
    }

    private var destructiveSection: some View {
        Section {
            Button("Sign out", action: onSignOut)
            Button("Delete account", role: .destructive, action: onDelete)
        } footer: {
            Text("Deleting your account permanently removes your saved events, ratings, and comments. This cannot be undone.")
        }
    }

    private func cityTitle(_ city: CitySummary) -> String {
        if let region = city.region, !region.isEmpty {
            return "\(city.name), \(region)"
        }
        return city.name
    }
}

private struct PasswordSection: View {
    @Bindable var viewModel: ProfileViewModel
    @State private var currentPassword = ""
    @State private var newPassword = ""
    @State private var confirmPassword = ""

    var body: some View {
        Section("Change Password") {
            SecureField("Current password", text: $currentPassword)
                .textContentType(.password)
            SecureField("New password", text: $newPassword)
                .textContentType(.newPassword)
            SecureField("Confirm new password", text: $confirmPassword)
                .textContentType(.newPassword)
            Button(viewModel.isChangingPassword ? "Updating..." : "Update password") {
                Task {
                    let changed = await viewModel.changePassword(
                        currentPassword: currentPassword,
                        newPassword: newPassword,
                        confirmPassword: confirmPassword
                    )
                    if changed {
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                    }
                }
            }
            .disabled(viewModel.isChangingPassword)
        }
    }
}
