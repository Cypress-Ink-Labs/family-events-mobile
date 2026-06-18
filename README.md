# Family Events — Mobile

Native consumer apps for Family Events, extracted from the original monorepo to
operate standalone. Two independent platforms:

- **iOS** — SwiftUI, Swift 5.10, iOS 17+, local Swift Package Manager modules.
  See [`ios/CLAUDE.md`](ios/CLAUDE.md).
- **Android** — Kotlin, Jetpack Compose, Gradle multi-module.
  See [`android/README.md`](android/README.md).

Both are **consumer-only** (no admin surfaces) and talk to a Supabase backend.

## Layout

| Path | What |
|------|------|
| `ios/` | iOS app + local SPM packages (`ios/Packages/`) |
| `android/` | Android Gradle modules |
| `tests/guards/` | Cross-cutting scope guards (`node --test tests/guards/`) |
| `.github/workflows/` | CI: scope guards, CodeQL, releases, token sync |

> This repo does **not** use a pnpm workspace. Commands run per-platform — there
> is no root `package.json`. Ignore monorepo-era `pnpm --filter` / `apps/...`
> references if you find them.

## Quick start

**iOS** (macOS + full Xcode required):
```bash
cd ios
pnpm run generate      # xcodegen generate
pnpm run test          # swift package tests + app tests
```

**Android** (JDK 17 + Android SDK):
```bash
cd android
pnpm run check         # ./gradlew check via scripts/with-android-env.sh
```

**Scope guards** (no toolchain needed):
```bash
node --test tests/guards/
```

## Environment

Copy `.env.example` to `.env` and fill in values (see that file for variable
names and where to obtain each). `.env` is git-ignored.
