import SwiftUI
import FEPlan
import FEExplore
import FESaved

public struct RootView: View {
    public static let shownTabs: [AppTab] = AppTab.allCases
    public let initialTab: AppTab

    @State private var selectedTab: AppTab

    public init(initialTab: AppTab = .plan) {
        self.initialTab = initialTab
        _selectedTab = State(initialValue: initialTab)
    }

    public var body: some View {
        TabView(selection: $selectedTab) {
            PlanTab()
                .tabItem { Label(AppTab.plan.title, systemImage: AppTab.plan.systemImage) }
                .tag(AppTab.plan)

            ExploreTab()
                .tabItem { Label(AppTab.explore.title, systemImage: AppTab.explore.systemImage) }
                .tag(AppTab.explore)

            SavedTab()
                .tabItem { Label(AppTab.saved.title, systemImage: AppTab.saved.systemImage) }
                .tag(AppTab.saved)
        }
    }
}

#Preview { RootView() }
