# ios — Claude Code Context

Root agent instructions: [`../../AGENTS.md`](../../AGENTS.md). This file scopes Claude to the iOS workspace only.

## Quick Reference

- **Platform**: iOS 17.0+ (SwiftUI)
- **Language**: Swift 5.10 (`SWIFT_VERSION` in `project.yml`; not Swift 6 yet)
- **UI**: SwiftUI with `@Observable`, `NavigationStack`, SwiftData persistence
- **Project gen**: XcodeGen — `ios/project.yml` is the source of truth, NOT `FamilyEvents.xcodeproj`
- **Package manager**: Swift Package Manager (local + pinned remote)
- **Backend**: Supabase Swift SDK + Google Sign-In SPM
- **Bundle ID**: `com.familyevents.app`
- **Consumer-only**: admin endpoints are blocked at runtime via `EndpointPolicyTests` — do not call admin RPCs from iOS targets

## Build Configurations (schemes)

Switch via Xcode scheme picker:

| Scheme | Config | Supabase | APP_WEB_URL |
|--------|--------|----------|-------------|
| `FamilyEvents` | `Debug` | `http://127.0.0.1:55321` (requires a local Supabase stack — see Gotchas) | `http://localhost:5173` |
| `FamilyEvents-Cloud` | `DebugCloud` | hosted dev project | Railway preview |
| Release | `Release` | hosted prod | `https://family-events.org` |

## Project Structure

```bash
ios/
├── project.yml              # XcodeGen source of truth — edit this, not .xcodeproj
├── package.json             # pnpm scripts (generate, test:packages, test:app)
├── FamilyEvents/            # App target sources
│   ├── App/                 # FamilyEventsApp, RootView, AppRoute, DeepLinkRouter, Tab
│   ├── FEAdmin/             # Admin-adjacent app glue (admin Packages are NOT linked here)
│   ├── FEAuth/              # Auth views/wiring at app level
│   ├── FECore/, FEData/, …  # App-level adapters per module
│   └── Assets.xcassets/
├── Packages/                # Local SPM packages (one product per dir)
│   ├── FECore/              # Env config, errors, keychain, primitives
│   ├── FEData/              # Supabase client, repos, SwiftData ModelContainer
│   ├── FEDesignSystem/      # Generated Tokens.swift, fonts, chrome appearance
│   ├── FEAuth/              # SupabaseAuthService, SessionStore, Google sign-in
│   ├── FEPlan/              # Plan feature module (composer, views, VMs)
│   ├── FEExplore/           # Explore tab
│   ├── FESaved/             # Saved/favorites tab
│   ├── FEEventDetail/       # Event detail flow
│   └── FEAppIntents/        # App Intents / Shortcuts
├── FamilyEventsTests/       # App-target XCTest (smoke + endpoint policy + routing)
└── FamilyEventsUITests/     # UITests (kept minimal — see DO NOT)
```

Per-package layout: `Sources/<Module>/`, `Tests/<Module>Tests/`, own `Package.swift`.

## XcodeBuildMCP Integration

When `XcodeBuildMCP` is configured, prefer it for build/test/run:

- Build sim: `mcp__xcodebuildmcp__build_sim_name_proj` (project `ios/FamilyEvents.xcodeproj`, scheme `FamilyEvents`, sim `iPhone 17`)
- Test sim: `mcp__xcodebuildmcp__test_sim_name_proj`
- Swift package build/test: `mcp__xcodebuildmcp__swift_package_build`, `mcp__xcodebuildmcp__swift_package_test`
- Boot/install/launch/logs/screenshot: `mcp__xcodebuildmcp__{boot_simulator,install_app,launch_app,capture_logs,screenshot}`
- Clean before suspicious failures: `mcp__xcodebuildmcp__clean`

If MCP is unavailable, fall back to pnpm scripts (below) — both work; the MCP tools just give faster feedback loops.

## Key Commands

Run from `ios/`:

```bash
pnpm run generate         # xcodegen generate — REQUIRED after editing project.yml
pnpm run test:packages    # swift test for each Package in Packages/
pnpm run test:app         # xcodegen + xcodebuild test (needs full Xcode selected)
pnpm run test             # both of the above
pnpm run clean            # nuke .build and xcuserdata
```

This standalone repo has no root pnpm workspace and no root-level iOS aggregate
scripts. Run the iOS scripts from `ios/`.

Per-package iteration (fastest):

```bash
cd Packages/FEPlan && swift test
```

## Coding Conventions

### Swift / SwiftUI

- `@Observable` over `ObservableObject`; use `@Bindable` to bind into them
- `async`/`await` for all async work; surface `AppError` (`FECore`) at boundaries
- Prefer value types; use `guard` for early exits
- `NavigationStack` only — repo has zero `NavigationView`
- Extract subviews when files exceed ~150 lines or a view body exceeds ~80 lines
- Never force-unwrap (`!`) without an inline comment explaining the invariant
- All persistence flows through `AppModelContainer` (`FEData`); never instantiate `ModelContainer` directly inside a view

### Module Boundaries

- App target depends on `FECore`, `FEData`, `FEDesignSystem`, `FEAuth`, `FEPlan`, `FEExplore`, `FESaved`, `FEEventDetail`, `FEAppIntents`. New feature → new local package, not a folder under `FamilyEvents/`.
- `FEDesignSystem.Tokens` is **generated** from the external `@cypress-ink-labs/design-system` package and synced by the `sync-tokens` GitHub Actions workflow. Do not hand-edit `Tokens.swift`; token changes are made upstream in the design-system package.
- Admin endpoints: blocked by `FamilyEventsTests/EndpointPolicyTests.swift`. Do not add admin RPC calls.

### Bootstrap pattern

`FamilyEvents/App/FamilyEventsApp.swift` builds dependencies once in `bootstrap()` and injects via `RootView` init + `.environment(sessionStore)`. New top-level services should be wired the same way — not via singletons inside views.

## Testing

- `swift-testing` is available but the suite is currently XCTest; match the file you're editing.
- ViewModel/business logic → per-package `Tests/<Module>Tests/`.
- App-level routing / policy → `FamilyEventsTests/`.
- UITests target `FamilyEventsUITests/` — minimal by design (see DO NOT).
- After any change to `project.yml`, packages, or build settings: regenerate, then run both `test:packages` and `test:app`.

## DO NOT

- Edit `FamilyEvents.xcodeproj` directly — regenerate from `project.yml`.
- Hand-edit `Tokens.swift` or any `*.generated.*` file.
- Add UITests during scaffolding — only for critical user flows after VMs are tested.
- Use `UIKit` shims where SwiftUI suffices; existing UIKit usage is limited to `UINavigationBar`/`UITabBar` appearance in `FEDesignSystem.configureChromeAppearance()`.
- Add admin or service-role endpoints; iOS is consumer-only.
- Introduce `ObservableObject` to a file that already uses `@Observable`.
- Suppress Swift 6 concurrency warnings — fix them at the source.
- Commit `Packages/*/.build/`, `.swiftpm/`, or `xcuserdata/`.

## Gotchas

- Local Supabase setup (`supabase start`, `scripts/setup-local.sh`) lived in the original monorepo and is not part of this standalone repo. Use the `FamilyEvents-Cloud` scheme (hosted dev Supabase) for local development, or obtain the local-stack scripts from the team.
- iOS targets only build on macOS with full Xcode (`xcode-select -s /Applications/Xcode.app`). CLI-only systems will skip `test:app`.
- The Google Sign-In button auto-hides when `IOS_GOOGLE_CLIENT_ID` is missing from the build config — check `project.yml` first when sign-in looks broken.
- `Packages/FEAdmin` exists but is **not** added to the app target. Leave it that way (consumer-only constraint).
- App entitlements live in `FamilyEvents/App/FamilyEvents.entitlements`. Adding a capability → update both that file and `project.yml`.

## Planning / Thinking

For non-trivial work in this app, prefer:

1. Read the relevant package's `Sources/` and `Tests/` first.
2. Use `ultrathink` for architecture-spanning changes (e.g., adding a new feature package, navigation refactor, Supabase schema realignment).
3. Use Plan Mode (`Shift+Tab`) before touching `project.yml`, the bootstrap, or anything that crosses 3+ packages.
4. Land changes per-package where possible; the per-package `swift test` loop is 10–20× faster than full-app `xcodebuild test`.

## Skill Routing (inherited from root AGENTS.md)

iOS-specific triggers route to the same skills as the rest of the repo (`/investigate`, `/review`, `/ship`, `/qa`, etc.). When unsure, defer to `../../AGENTS.md` § "Skill routing".
