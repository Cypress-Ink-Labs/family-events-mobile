import SwiftUI
import FEDesignSystem

public struct SavedTab: View {
    public let tabTitle = "Saved"
    public let onOpenProfile: (() -> Void)?

    public init(onOpenProfile: (() -> Void)? = nil) {
        self.onOpenProfile = onOpenProfile
    }

    public var body: some View {
        NavigationStack {
            PlaceholderView(title: tabTitle, systemImage: "bookmark.fill")
                .navigationTitle(tabTitle)
                .toolbar {
                    if let onOpenProfile {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button { onOpenProfile() } label: {
                                Image(systemName: "person.crop.circle")
                            }
                        }
                    }
                }
        }
    }
}
