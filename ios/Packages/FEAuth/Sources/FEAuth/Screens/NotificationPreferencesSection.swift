import SwiftUI
import FEData

struct NotificationPreferencesSection: View {
    var viewModel: NotificationPreferencesViewModel

    var body: some View {
        Section("Reminders") {
            Toggle("Email reminders", isOn: binding(\.reminderEmail))
            Toggle("Push reminders", isOn: binding(\.reminderPush))
        }
        Section("Event Changes") {
            Toggle("Email updates", isOn: binding(\.changeEmail))
            Toggle("Push updates", isOn: binding(\.changePush))
        }
        Section("Weekly Digest") {
            Toggle("Email digest", isOn: binding(\.digestEmail))
            Toggle("Push digest", isOn: binding(\.digestPush))
        }
        if let errorMessage = viewModel.errorMessage {
            Section {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        }
    }

    private func binding(_ keyPath: WritableKeyPath<NotificationPreferences, Bool>) -> Binding<Bool> {
        Binding(
            get: { viewModel.preferences[keyPath: keyPath] },
            set: { newValue in Task { await viewModel.set(keyPath, to: newValue) } }
        )
    }
}
