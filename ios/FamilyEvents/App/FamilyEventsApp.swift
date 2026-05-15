import SwiftUI
import SwiftData
import FECore
import FEData
import FEAuth
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
            modelContainer: ModelContainer
        )
        case configError(String)
    }
    private let boot: BootResult

    init() { boot = Self.bootstrap() }

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
            return .ready(
                authService: svc,
                sessionStore: store,
                composer: composer,
                profileRepo: profileRepo,
                cityRepo: cityRepo,
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
            case .ready(let authService, let sessionStore, let composer, let profileRepo, let cityRepo, let modelContainer):
                RootView(authService: authService, planComposer: composer, profileRepo: profileRepo, cityRepo: cityRepo)
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
