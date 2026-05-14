import SwiftUI
import FECore
import FEData
import FEAuth

@main
struct FamilyEventsApp: App {
    private let env: EnvConfig
    private let supabase: FamilyEventsSupabase
    private let authService: any AuthService
    @State private var sessionStore: SessionStore

    init() {
        do {
            let env = try EnvConfig.load()
            self.env = env
            let supa = FamilyEventsSupabase(config: env)
            self.supabase = supa
            let svc = SupabaseAuthService(supabase: supa)
            self.authService = svc
            _sessionStore = State(initialValue: SessionStore(
                authService: svc,
                storage: SecItemKeychainStorage(service: "com.familyevents.app.auth")
            ))
        } catch {
            fatalError("EnvConfig failed to load: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            RootView(authService: authService)
                .environment(sessionStore)
        }
    }
}
