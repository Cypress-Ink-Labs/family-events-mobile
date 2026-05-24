# apps/android

Scope: native Kotlin/Jetpack Compose consumer Android app.

## Commands

Run from repo root:

```bash
pnpm --filter @family-events/android check
pnpm --filter @family-events/android test
pnpm --filter @family-events/android build
pnpm --filter @family-events/android lint
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
apps/android/designsystem/src/main/java/com/familyevents/designsystem/generated/Tokens.kt
```

Change `packages/design-system/tokens/tokens.json`, then run:

```bash
pnpm --filter @family-events/design-system build
```

## Verification

For Android-only changes:

```bash
pnpm run verify:android
```

For shared contract or design-token changes, also run:

```bash
pnpm run verify:web
```
