import SwiftUI
import FEDesignSystem

public struct PlanTab: View {
    public let tabTitle = "Plan"

    public init() {}

    public var body: some View {
        NavigationStack {
            PlaceholderView(title: tabTitle, systemImage: "calendar.badge.clock")
                .navigationTitle(tabTitle)
        }
    }
}
