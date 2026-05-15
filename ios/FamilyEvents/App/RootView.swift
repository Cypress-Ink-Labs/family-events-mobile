import SwiftUI
import FECore
import FEData
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
    private let planComposer: PlanComposer
    private let profileRepo: any ProfileRepo

    @Environment(SessionStore.self) private var sessionStore
    @State private var selectedTab: AppTab
    @State private var pendingResetToken: PendingResetToken?
    @State private var showProfile = false
    @State private var planContext: PlanContext?

    init(
        authService: any AuthService,
        planComposer: PlanComposer,
        profileRepo: any ProfileRepo,
        initialTab: AppTab = .plan
    ) {
        self.authService = authService
        self.planComposer = planComposer
        self.profileRepo = profileRepo
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
            case .signedIn(let userID):
                signedInContent(userID: userID)
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
        .sheet(isPresented: $showProfile) {
            ProfileSheet(authService: authService)
        }
        .sheet(item: $pendingResetToken) { pending in
            NavigationStack {
                ResetPasswordScreen(
                    viewModel: ResetPasswordViewModel(token: pending.token, authService: authService, sessionStore: sessionStore),
                    onDone: { pendingResetToken = nil }
                )
            }
        }
    }

    @ViewBuilder
    private func signedInContent(userID: UserID) -> some View {
        if let ctx = planContext {
            TabView(selection: $selectedTab) {
                PlanTab(composer: planComposer, context: ctx, cityName: nil, onSelectEvent: { _ in })
                    .tabItem { Label(AppTab.plan.title, systemImage: AppTab.plan.systemImage) }
                    .tag(AppTab.plan)
                ExploreTab()
                    .tabItem { Label(AppTab.explore.title, systemImage: AppTab.explore.systemImage) }
                    .tag(AppTab.explore)
                SavedTab(onOpenProfile: { showProfile = true })
                    .tabItem { Label(AppTab.saved.title, systemImage: AppTab.saved.systemImage) }
                    .tag(AppTab.saved)
            }
        } else {
            ProgressView()
                .controlSize(.large)
                .task(id: userID) {
                    planContext = await resolveContext(userID: userID)
                }
        }
    }

    private func resolveContext(userID: UserID) async -> PlanContext {
        do {
            let (cityID, kidAge) = try await profileRepo.currentContext(userID: userID)
            return PlanContext(userID: userID, cityID: cityID, kidAge: kidAge)
        } catch {
            return PlanContext(userID: userID, cityID: nil, kidAge: nil)
        }
    }
}
