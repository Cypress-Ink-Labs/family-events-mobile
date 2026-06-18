# android

Scope: native Kotlin/Jetpack Compose consumer Android app.

## Commands

Run from `android/` — either the pnpm scripts (which wrap
`scripts/with-android-env.sh` to locate JDK 17 + the SDK) or gradle directly:

```bash
pnpm run check     # or: ./gradlew check
pnpm run test      # or: ./gradlew test
pnpm run build     # or: ./gradlew assembleDebug
pnpm run lint      # or: ./gradlew lint
```

## Boundaries

- Consumer-only: Plan, Explore, Saved, Event Detail, Auth, Profile.
- Admin surfaces stay out of Android unless explicitly approved.
- `:core` owns domain primitives and pure helpers.
- `:data` owns Supabase, Ktor, Room, DTOs, mappers, and repository implementations.
- `:designsystem` owns Compose UI primitives and generated design tokens.
- Feature modules consume `:core`, repository contracts from `:data`, and `:designsystem`.
- Feature modules must not import Supabase, Ktor, or Room directly.
- `:app` owns app assembly, root navigation, dependency wiring, and build config.

## Generated Tokens

Do not hand-edit:

```text
android/designsystem/src/main/java/com/familyevents/designsystem/generated/Tokens.kt
```

Tokens are generated from the external `@cypress-ink-labs/design-system` package
and synced by the `sync-tokens` GitHub Actions workflow. Token changes are made
upstream in the design-system package, not here.

## Verification

Verify Android changes from `android/`:

```bash
pnpm run check
```

No cross-platform aggregate verify task exists in this standalone repo.
