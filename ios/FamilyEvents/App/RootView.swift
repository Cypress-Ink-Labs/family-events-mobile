import SwiftUI
import FECore
import FEAuth
import FEPlan
import FEExplore
import FESaved

private struct PendingResetToken: Identifiable, Equatable {
    let id: String
    var token: String { id }
}

struct RootView: View {
    static let shownTabs: [AppTab] = AppTab.allCases
    let initialTab: AppTab
    private let authService: any AuthService

    @Environment(SessionStore.self) private var sessionStore
    @State private var selectedTab: AppTab
    @State private var pendingResetToken: PendingResetToken?

    init(authService: any AuthService, initialTab: AppTab = .plan) {
        self.authService = authService
        self.initialTab = initialTab
        _selectedTab = State(initialValue: initialTab)
    }

    var body: some View {
        Group {
            switch sessionStore.state {
            case .hydrating:
                ProgressView().controlSize(.large)
            case .signedOut, .linkRequired:
                AuthRootView(authService: authService)
            case .signedIn:
                TabView(selection: $selectedTab) {
                    PlanTab().tabItem { Label(AppTab.plan.title, systemImage: AppTab.plan.systemImage) }.tag(AppTab.plan)
                    ExploreTab().tabItem { Label(AppTab.explore.title, systemImage: AppTab.explore.systemImage) }.tag(AppTab.explore)
                    SavedTab().tabItem { Label(AppTab.saved.title, systemImage: AppTab.saved.systemImage) }.tag(AppTab.saved)
                }
            }
        }
        .onOpenURL { url in
            if let result = DeepLinkRouter.route(from: url) {
                for route in result.routes {
                    if case .resetPassword(let token) = route {
                        pendingResetToken = PendingResetToken(id: token)
                    }
                }
            }
        }
        .sheet(item: $pendingResetToken) { pending in
            NavigationStack {
                ResetPasswordScreen(
                    viewModel: ResetPasswordViewModel(token: pending.token, authService: authService),
                    onDone: { pendingResetToken = nil }
                )
            }
        }
    }
}
