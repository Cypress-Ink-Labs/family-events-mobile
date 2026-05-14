import SwiftUI
import FEDesignSystem

public struct ExploreTab: View {
    public let tabTitle = "Explore"

    public init() {}

    public var body: some View {
        NavigationStack {
            PlaceholderView(title: tabTitle, systemImage: "sparkle.magnifyingglass")
                .navigationTitle(tabTitle)
        }
    }
}
