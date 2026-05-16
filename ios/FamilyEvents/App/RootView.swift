import SwiftUI
import SwiftData
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

/// No-op fallback used when no real CityRepository is injected (e.g., previews, tests).
private struct FallbackCityRepository: CityRepository {
    func cityName(id: CityID) async throws -> String? { nil }
}

/// No-op fallback for EventRepository (previews, tests).
private struct FallbackEventRepository: EventRepository {
    func fetch(ids: [EventID], for userID: UserID) async throws -> [EventDTO] { [] }
    func fetchList(query: EventQuery, for userID: UserID) async throws -> [EventDTO] { [] }
}

/// No-op fallback for FavoriteRepo (previews, tests).
private struct FallbackFavoriteRepo: FavoriteRepo {
    func favorites(for userID: UserID) async throws -> [FavoriteDTO] { [] }
    func favorite(eventID: EventID, for userID: UserID) async throws {}
    func unfavorite(eventID: EventID, for userID: UserID) async throws {}
    func observeFavorites(for userID: UserID) -> AsyncStream<FavoriteChange> {
        AsyncStream { _ in }
    }
}

struct RootView: View {
    static let shownTabs: [AppTab] = AppTab.allCases
    let initialTab: AppTab
    private let authService: any AuthService
    private let planComposer: PlanComposer
    private let profileRepo: any ProfileRepo
    private let cityRepo: any CityRepository
    private let eventRepo: any EventRepository
    private let favoriteRepo: any FavoriteRepo
    private let modelContainer: ModelContainer?

    @Environment(SessionStore.self) private var sessionStore
    @State private var selectedTab: AppTab
    @State private var pendingResetToken: PendingResetToken?
    @State private var showProfile = false
    @State private var planContext: PlanContext?
    @State private var cityName: String?
    @State private var showCityPicker = false

    init(
        authService: any AuthService,
        planComposer: PlanComposer,
        profileRepo: any ProfileRepo,
        cityRepo: any CityRepository = FallbackCityRepository(),
        eventRepo: any EventRepository = FallbackEventRepository(),
        favoriteRepo: any FavoriteRepo = FallbackFavoriteRepo(),
        modelContainer: ModelContainer? = nil,
        initialTab: AppTab = .plan
    ) {
        self.authService = authService
        self.planComposer = planComposer
        self.profileRepo = profileRepo
        self.cityRepo = cityRepo
        self.eventRepo = eventRepo
        self.favoriteRepo = favoriteRepo
        self.modelContainer = modelContainer
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
        .sheet(isPresented: $showCityPicker) {
            CityPickerStub(onDismiss: { showCityPicker = false })
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
                PlanTab(
                    composer: planComposer,
                    eventRepo: eventRepo,
                    favoriteRepo: favoriteRepo,
                    context: ctx,
                    cityName: cityName,
                    onSetCity: { showCityPicker = true }
                )
                    .tabItem { Label(AppTab.plan.title, systemImage: AppTab.plan.systemImage) }
                    .tag(AppTab.plan)
                ExploreTab(eventRepo: eventRepo, favoriteRepo: favoriteRepo, userID: userID, cityID: ctx.cityID)
                    .tabItem { Label(AppTab.explore.title, systemImage: AppTab.explore.systemImage) }
                    .tag(AppTab.explore)
                if let container = modelContainer {
                    SavedTab(
                        favoriteRepo: favoriteRepo,
                        eventRepo: eventRepo,
                        modelContainer: container,
                        userID: userID,
                        onOpenProfile: { showProfile = true }
                    )
                        .tabItem { Label(AppTab.saved.title, systemImage: AppTab.saved.systemImage) }
                        .tag(AppTab.saved)
                }
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
            if let cityID {
                cityName = try? await cityRepo.cityName(id: cityID)
            }
            return PlanContext(userID: userID, cityID: cityID, kidAge: kidAge)
        } catch {
            return PlanContext(userID: userID, cityID: nil, kidAge: nil)
        }
    }
}
