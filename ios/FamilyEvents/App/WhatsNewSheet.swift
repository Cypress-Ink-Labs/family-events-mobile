import SwiftUI
import FEDesignSystem

/// One-shot onboarding sheet shown the first time a user opens the app
/// after the 5-tab parity upgrade. Gated by
/// `@AppStorage("seen-tabs-onboarding-v2")` so dismissal is permanent.
struct WhatsNewSheet: View {
    private static let storageKey = "seen-tabs-onboarding-v2"

    @AppStorage(storageKey) private var hasSeen: Bool = false
    @Environment(\.dismiss) private var dismiss

    static var shouldShow: Bool {
        !UserDefaults.standard.bool(forKey: storageKey)
    }

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "sparkles")
                .font(.system(size: 48))
                .foregroundStyle(Color.dsAccentSecondary)
                .padding(.top, 32)

            Text("New tabs unlocked")
                .font(.title2.weight(.semibold))
                .foregroundStyle(Color.dsTextPrimary)

            VStack(alignment: .leading, spacing: 16) {
                row(
                    icon: "map.fill",
                    title: "Map",
                    body: "See nearby family events on a map and tap a pin for details."
                )
                row(
                    icon: "calendar",
                    title: "Calendar",
                    body: "Browse the month at a glance and drill into any day's events."
                )
            }
            .padding(.horizontal, 24)

            Spacer()

            Button {
                hasSeen = true
                dismiss()
            } label: {
                Text("Got it")
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color.dsAccentPrimary)
            .padding(.horizontal, 32)
            .padding(.bottom, 24)
        }
        .background(Color.dsBackground)
    }

    private func row(icon: String, title: String, body: String) -> some View {
        HStack(alignment: .top, spacing: 14) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundStyle(Color.dsAccentPrimary)
                .frame(width: 36)
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(Color.dsTextPrimary)
                Text(body)
                    .font(.subheadline)
                    .foregroundStyle(Color.dsTextMuted)
            }
        }
    }
}
