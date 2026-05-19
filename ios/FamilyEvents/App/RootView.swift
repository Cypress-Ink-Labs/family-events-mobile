import SwiftUI
import SwiftData
import FECore
import FEData
import FEAuth
import FEPlan
import FEExplore
import FESaved
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
    static let shownTabs: [AppTab] = [.plan, .explore, .saved]
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
    @State private var selectedTab: AppTab
    @State private var pendingResetToken: PendingResetToken?
    @State private var showProfile = false
    @State private var planContext: PlanContext?
    @State private var cityName: String?
    @State private var isAdmin = false

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
                if isAdmin {
                    AdminTab()
                        .tabItem { Label(AppTab.admin.title, systemImage: AppTab.admin.systemImage) }
                        .tag(AppTab.admin)
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
            let profile = try await profileRepo.profile(userID: userID)
            isAdmin = profile?.isAdmin == true
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

private struct AdminTab: View {
    private let sections: [(String, String)] = [
        ("Dashboard", "Events, sources, review load, and AI confidence."),
        ("Sources", "Scrape sources, runs, errors, and refresh actions."),
        ("Events", "Search, edit, publish, lock, and delete events."),
        ("Cities", "Markets, active status, timezone, and source coverage."),
        ("Comments", "Approve, flag, and moderate event comments."),
        ("Ratings", "Review and remove event ratings."),
        ("Access", "User access, role, and account enablement."),
        ("Invites", "Invite requests, codes, approval, and revocation."),
        ("Logs", "Source run logs, tag queue, and audit history."),
        ("Crons", "Scheduled jobs, run history, and manual triggers."),
    ]

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: DesignTokens.Space.s4) {
                    VStack(alignment: .leading, spacing: DesignTokens.Space.s1) {
                        Text("Admin")
                            .font(.dsTitleLg)
                            .foregroundStyle(Color.dsTextPrimary)
                        Text("Native parity shell for the web back office.")
                            .font(.dsBodySm)
                            .foregroundStyle(Color.dsTextMuted)
                    }
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: DesignTokens.Space.s3) {
                        metric("Events", "—")
                        metric("Drafts", "—")
                        metric("Sources", "—")
                        metric("Errors", "—")
                    }
                    ForEach(sections, id: \.0) { title, body in
                        VStack(alignment: .leading, spacing: DesignTokens.Space.s1) {
                            Text(title)
                                .font(.dsTitleLg)
                                .foregroundStyle(Color.dsTextPrimary)
                            Text(body)
                                .font(.dsBodySm)
                                .foregroundStyle(Color.dsTextMuted)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(DesignTokens.Space.s4)
                        .background(Color.dsSurface)
                        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.md))
                        .overlay(
                            RoundedRectangle(cornerRadius: DesignTokens.Radius.md)
                                .stroke(Color.dsBorder, lineWidth: 1)
                        )
                    }
                }
                .padding(DesignTokens.Space.s4)
            }
            .background(Color.dsBackground)
            .navigationTitle("Admin")
        }
    }

    private func metric(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: DesignTokens.Space.s1) {
            Text(value)
                .font(.dsTitleLg)
            Text(label)
                .font(.dsCaptionXs)
                .foregroundStyle(Color.dsTextMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(DesignTokens.Space.s3)
        .background(Color.dsSurfaceRaised)
        .clipShape(RoundedRectangle(cornerRadius: DesignTokens.Radius.sm))
    }
}
