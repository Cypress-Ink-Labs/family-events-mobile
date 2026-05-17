import SwiftUI
import SwiftData
import FECore
import FEData
import FEAuth
import FEDesignSystem
import FEPlan

@main
struct FamilyEventsApp: App {
    private enum BootResult {
        case ready(
            authService: any AuthService,
            sessionStore: SessionStore,
            composer: PlanComposer,
            profileRepo: any ProfileRepo,
            cityRepo: any CityRepository,
            eventRepo: any EventRepository,
            favoriteRepo: any FavoriteRepo,
            modelContainer: ModelContainer
        )
        case configError(String)
    }
    private let boot: BootResult

    init() {
        // Register bundled fonts before any view renders. Idempotent — re-runs
        // safely on hot-reload. Failures are non-fatal; missing fonts fall back
        // to system serif/sans equivalents.
        FEDesignSystem.registerFonts()
        // Apply warm-paper opaque appearance to UINavigationBar + UITabBar so
        // scroll content doesn't ghost behind translucent system chrome.
        FEDesignSystem.configureChromeAppearance()
        boot = Self.bootstrap()
    }

    private static func bootstrap() -> BootResult {
        do {
            let env = try EnvConfig.load()
            let supa = FamilyEventsSupabase(config: env)
            let svc = SupabaseAuthService(supabase: supa)
            let store = SessionStore(
                authService: svc,
                storage: SecItemKeychainStorage(service: "com.familyevents.app.auth")
            )
            let container = try AppModelContainer.makePersistent()
            let composer = PlanModule.makeComposer(supabase: supa, modelContainer: container)
            let profileRepo = SupabaseProfileRepo(supabase: supa)
            let cityRepo = SupabaseCityRepository(supabase: supa)
            let eventRepo = SupabaseEventRepository(supabase: supa)
            let favoriteRepo = SupabaseFavoriteRepo(supabase: supa)
            return .ready(
                authService: svc,
                sessionStore: store,
                composer: composer,
                profileRepo: profileRepo,
                cityRepo: cityRepo,
                eventRepo: eventRepo,
                favoriteRepo: favoriteRepo,
                modelContainer: container
            )
        } catch let error as AppError {
            return .configError(error.userMessage)
        } catch {
            return .configError("Configuration error: \(error.localizedDescription)")
        }
    }

    var body: some Scene {
        WindowGroup {
            switch boot {
            case .ready(let authService, let sessionStore, let composer, let profileRepo, let cityRepo, let eventRepo, let favoriteRepo, let modelContainer):
                RootView(
                    authService: authService,
                    planComposer: composer,
                    profileRepo: profileRepo,
                    cityRepo: cityRepo,
                    eventRepo: eventRepo,
                    favoriteRepo: favoriteRepo,
                    modelContainer: modelContainer
                )
                .environment(sessionStore)
                .modelContainer(modelContainer)   // D14b: same instance the composer holds
            case .configError(let message):
                ConfigErrorView(message: message)
            }
        }
    }
}

private struct ConfigErrorView: View {
    let message: String
    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.system(size: 48)).foregroundStyle(.orange)
            Text("Couldn't start the app").font(.title2.weight(.semibold))
            Text(message).font(.body).foregroundStyle(.secondary).multilineTextAlignment(.center)
        }.padding()
    }
}
