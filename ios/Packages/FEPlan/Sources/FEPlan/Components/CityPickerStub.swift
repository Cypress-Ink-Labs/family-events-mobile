import SwiftUI

/// Placeholder for the M7 city picker. Showing this satisfies the M3.5
/// "set your city" CTA contract; the real list-search UI lands later.
public struct CityPickerStub: View {
    public let onDismiss: () -> Void

    public init(onDismiss: @escaping () -> Void) {
        self.onDismiss = onDismiss
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Image(systemName: "mappin.and.ellipse")
                    .font(.system(size: 64))
                    .foregroundStyle(.tint)
                Text("City picker")
                    .font(.title2.weight(.semibold))
                Text("Pick from a list of cities — coming in a future update.")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                    .padding(.horizontal)
            }
            .padding()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close", action: onDismiss)
                }
            }
        }
    }
}
