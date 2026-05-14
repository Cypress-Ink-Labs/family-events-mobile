import SwiftUI
import FECore
import FEData
import FEAuth

@main
struct FamilyEventsApp: App {
    private enum BootResult {
        case ready(authService: any AuthService, sessionStore: SessionStore)
        case configError(String)
    }

    private let boot: BootResult

    init() {
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
            return .ready(authService: svc, sessionStore: store)
        } catch let error as AppError {
            return .configError(error.userMessage)
        } catch {
            return .configError("Configuration error: \(error.localizedDescription)")
        }
    }

    var body: some Scene {
        WindowGroup {
            switch boot {
            case .ready(let authService, let sessionStore):
                RootView(authService: authService)
                    .environment(sessionStore)
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
                .font(.system(size: 48))
                .foregroundStyle(.orange)
            Text("Couldn't start the app")
                .font(.title2.weight(.semibold))
            Text(message)
                .font(.body)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
    }
}
