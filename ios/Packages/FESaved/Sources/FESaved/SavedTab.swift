import SwiftUI
import FEDesignSystem

public struct SavedTab: View {
    public let tabTitle = "Saved"

    public init() {}

    public var body: some View {
        NavigationStack {
            PlaceholderView(title: tabTitle, systemImage: "bookmark.fill")
                .navigationTitle(tabTitle)
        }
    }
}
