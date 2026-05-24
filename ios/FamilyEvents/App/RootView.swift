import SwiftUI
import SwiftData
import FECore
import FEData
import FEAuth
import FEPlan
import FEExplore
import FESaved
import FEMap
import FECalendar
import FECityPicker
import FEDesignSystem

private struct PendingResetToken: Identifiable, Equatable {
    let id: String
    var token: String { id }
}

/// No-op fallback used when no real CityRepository is injected (e.g., previews, tests).
private struct FallbackCityRepository: CityRepository {
    func cities() async throws -> [CitySummary] { [] }
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
    static let shownTabs: [AppTab] = [.plan, .explore, .map, .calendar, .saved]
    let initialTab: AppTab
    private let authService: any AuthService
    private let planComposer: PlanComposer
    private let profileRepo: any ProfileRepo
    private let cityRepo: any CityRepository
    private let eventRepo: any EventRepository
    private let favoriteRepo: any FavoriteRepo
    private let ratingRepo: (any RatingRepo)?
    private let commentRepo: (any CommentRepo)?
    private let modelContainer: ModelContainer?
    private let googleSignInEnabled: Bool

    @Environment(SessionStore.self) private var sessionStore
    @Environment(\.scenePhase) private var scenePhase
    @State private var refreshController = ScenePhaseRefreshController()
    @State private var selectedTab: AppTab
    @State private var pendingResetToken: PendingResetToken?
    @State private var showProfile = false
    @State private var planContext: PlanContext?
    @State private var cityName: String?
    @State private var showWhatsNew = false

    init(
        authService: any AuthService,
        planComposer: PlanComposer,
        profileRepo: any ProfileRepo,
        cityRepo: any CityRepository = FallbackCityRepository(),
        eventRepo: any EventRepository = FallbackEventRepository(),
        favoriteRepo: any FavoriteRepo = FallbackFavoriteRepo(),
        ratingRepo: (any RatingRepo)? = nil,
        commentRepo: (any CommentRepo)? = nil,
        modelContainer: ModelContainer? = nil,
        googleSignInEnabled: Bool = false,
        initialTab: AppTab = .plan
    ) {
        self.authService = authService
        self.planComposer = planComposer
        self.profileRepo = profileRepo
        self.cityRepo = cityRepo
        self.eventRepo = eventRepo
        self.favoriteRepo = favoriteRepo
        self.ratingRepo = ratingRepo
        self.commentRepo = commentRepo
        self.modelContainer = modelContainer
        self.googleSignInEnabled = googleSignInEnabled
        self.initialTab = initialTab
        _selectedTab = State(initialValue: initialTab)
    }

    var body: some View {
        Group {
            switch sessionStore.state {
            case .hydrating:
                ProgressView().controlSize(.large)
            case .signedOut, .linkRequired:
                AuthRootView(authService: authService, googleSignInEnabled: googleSignInEnabled)
            case .signedIn(let userID):
                signedInContent(userID: userID)
            }
        }
        .onOpenURL { url in
            // GoogleSignIn must see the auth callback URL first; if it claims the
            // URL (returns true), don't fall through to DeepLinkRouter.
            if GoogleSignInCoordinator.handle(url: url) { return }
            if let result = DeepLinkRouter.route(from: url) {
                selectedTab = result.tab
                for route in result.routes {
                    if case .resetPassword(let token) = route {
                        pendingResetToken = PendingResetToken(id: token)
                    }
                }
            }
        }
        .environment(refreshController)
        .onChange(of: scenePhase) { _, newPhase in
            switch newPhase {
            case .active:
                refreshController.scenePhaseChanged(.active)
            case .inactive:
                refreshController.scenePhaseChanged(.inactive)
            case .background:
                refreshController.scenePhaseChanged(.background)
            @unknown default:
                break
            }
        }
        .sheet(isPresented: $showProfile) {
            ProfileSheet(
                authService: authService,
                profileRepo: profileRepo,
                cityRepo: cityRepo,
                onProfileSaved: { profile in
                    Task { await applyProfile(profile) }
                }
            )
        }
        .sheet(item: $pendingResetToken) { pending in
            NavigationStack {
                ResetPasswordScreen(
                    viewModel: ResetPasswordViewModel(token: pending.token, authService: authService, sessionStore: sessionStore),
                    onDone: { pendingResetToken = nil }
                )
            }
        }
        .sheet(isPresented: $showWhatsNew) {
            WhatsNewSheet()
        }
        .onAppear {
            if case .signedIn = sessionStore.state, WhatsNewSheet.shouldShow {
                showWhatsNew = true
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
                    ratingRepo: ratingRepo,
                    commentRepo: commentRepo,
                    context: ctx,
                    cityName: cityName,
                    onSetCity: { showProfile = true }
                )
                    .tabItem { Label(AppTab.plan.title, systemImage: AppTab.plan.systemImage) }
                    .tag(AppTab.plan)
                ExploreTab(
                    eventRepo: eventRepo,
                    favoriteRepo: favoriteRepo,
                    ratingRepo: ratingRepo,
                    commentRepo: commentRepo,
                    userID: userID,
                    cityID: ctx.cityID
                )
                    .tabItem { Label(AppTab.explore.title, systemImage: AppTab.explore.systemImage) }
                    .tag(AppTab.explore)
                MapTab(
                    eventRepo: eventRepo,
                    cityRepo: cityRepo,
                    favoriteRepo: favoriteRepo,
                    ratingRepo: ratingRepo,
                    commentRepo: commentRepo,
                    userID: userID,
                    cityID: ctx.cityID,
                    cityName: cityName,
                    onCityChanged: { city in
                        Task { await applyCityChange(city, userID: userID) }
                    }
                )
                    .tabItem { Label(AppTab.map.title, systemImage: AppTab.map.systemImage) }
                    .tag(AppTab.map)
                CalendarTab(
                    eventRepo: eventRepo,
                    cityRepo: cityRepo,
                    favoriteRepo: favoriteRepo,
                    ratingRepo: ratingRepo,
                    commentRepo: commentRepo,
                    userID: userID,
                    cityID: ctx.cityID,
                    cityName: cityName,
                    onCityChanged: { city in
                        Task { await applyCityChange(city, userID: userID) }
                    }
                )
                    .tabItem { Label(AppTab.calendar.title, systemImage: AppTab.calendar.systemImage) }
                    .tag(AppTab.calendar)
                if let container = modelContainer {
                    SavedTab(
                        favoriteRepo: favoriteRepo,
                        eventRepo: eventRepo,
                        ratingRepo: ratingRepo,
                        commentRepo: commentRepo,
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

    private func applyCityChange(_ city: CitySummary, userID: UserID) async {
        cityName = city.name
        planContext = PlanContext(
            userID: userID,
            cityID: city.id,
            kidAge: planContext?.kidAge
        )
        // Best-effort persist to the user profile so the choice survives
        // across cold restart and other devices.
        _ = try? await profileRepo.updateProfile(
            UserProfileUpdate(
                displayName: nil,
                cityPreferenceID: city.id,
                childName: nil,
                childAge: planContext?.kidAge
            ),
            for: userID
        )
    }

    private func resolveContext(userID: UserID) async -> PlanContext {
        do {
            let profile = try await profileRepo.profile(userID: userID)
            let cityID = profile?.cityPreferenceID
            let kidAge = profile?.childAge
            if let cityID {
                cityName = try? await cityRepo.cityName(id: cityID)
            }
            return PlanContext(userID: userID, cityID: cityID, kidAge: kidAge)
        } catch {
            return PlanContext(userID: userID, cityID: nil, kidAge: nil)
        }
    }

    @MainActor
    private func applyProfile(_ profile: UserProfile) async {
        if let cityID = profile.cityPreferenceID {
            cityName = try? await cityRepo.cityName(id: cityID)
        } else {
            cityName = nil
        }
        planContext = PlanContext(
            userID: profile.id,
            cityID: profile.cityPreferenceID,
            kidAge: profile.childAge
        )
    }
}

