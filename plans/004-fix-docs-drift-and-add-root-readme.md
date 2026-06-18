# Plan 004: Fix monorepo-drift documentation and add a root README + .env.example

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- ios/CLAUDE.md ios/AGENTS.md android/AGENTS.md android/README.md`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: LOW (docs only)
- **Depends on**: none (but best done after 001-003 so the docs can reference the
  real, working commands)
- **Category**: docs / dx
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

Every developer- and agent-facing doc still describes the monorepo. They tell you
to run `pnpm --filter @family-events/android ...`, `pnpm run ios:test`,
`pnpm run verify:ios`, `supabase start` "from repo root", and to edit
`packages/design-system/tokens/tokens.json`. None of those work here: there is no
root `package.json`, no pnpm workspace, no `packages/`, no root `supabase`. A
contributor (or an executor agent following these docs) hits "No package.json
found" / "No projects matched the filters" immediately. There is also no root
README and no `.env.example`, so a fresh clone has no entry point and no record
of required environment variables. This plan corrects the four drifted docs and
adds the two missing entry-point files.

## Current state

**Commands that actually work in this repo:**
- iOS, from `ios/`: `pnpm run generate`, `pnpm run test:packages`,
  `pnpm run test:app`, `pnpm run test` (see `ios/package.json`). Per-package:
  `cd ios/Packages/FEPlan && swift test`.
- Android, from `android/`: `pnpm run check|test|build|lint` (each wraps
  `scripts/with-android-env.sh ./gradlew ...` — see `android/package.json`), or
  call gradle directly: `cd android && ./gradlew check`.
- Guards, from repo root: `node --test tests/guards/`.
- Design tokens: synced from the external npm package `@cypress-ink-labs/design-system`
  via `.github/workflows/sync-tokens.yml` (which writes the generated
  `Tokens.swift` / `Tokens.kt`). There is no local `packages/design-system`.

**Drifted doc lines (each references something absent in this repo):**
- `ios/CLAUDE.md`
  - Lines 78-83: "From repo root: `pnpm run ios:generate` / `pnpm run ios:test`".
  - Line 106: "change `tokens/tokens.json` and run `pnpm --filter @family-events/design-system build`".
  - Line 134: "Debug scheme needs ... `pnpm run db:start` + `bash scripts/setup-local.sh` from the repo root."
  - Several paths printed as `apps/ios/...` (e.g. the project-structure block).
- `ios/AGENTS.md`
  - Lines 13-18: "Run from repo root: `pnpm run ios:generate` / `pnpm run ios:test`".
  - Lines 42-50: edit `packages/design-system/tokens/tokens.json` + `pnpm --filter`.
  - Lines 52-65: "`pnpm run verify:ios` / `verify:web` / `verify:android`" (none exist).
  - Line 9 + token block: `apps/ios/...` paths.
- `android/AGENTS.md`
  - Lines 9-13: `pnpm --filter @family-events/android check|test|build|lint`.
  - Lines 30-40: edit `packages/design-system/tokens/tokens.json` + `pnpm --filter`.
  - Lines 43-53: `pnpm run verify:android` / `verify:web`.
- `android/README.md`
  - Lines 13-16: `pnpm --filter @family-events/android ...`.

**Missing files:** no `/README.md`, no `/.env.example`. Root `.env` exists but is
empty and git-ignored (`.gitignore` ignores `.env`, allows `.env.example`).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Confirm no root pkg | `ls package.json 2>&1` | "No such file or directory" |
| iOS scripts | `cat ios/package.json` | shows generate/test:packages/test:app/test |
| Android scripts | `cat android/package.json` | shows check/test/build/lint/clean |
| Find drift after fix | `grep -rn "pnpm --filter\|pnpm run ios:\|verify:ios\|verify:web\|verify:android\|packages/design-system\|pnpm run db:start" ios/CLAUDE.md ios/AGENTS.md android/AGENTS.md android/README.md` | no matches |

## Scope

**In scope** (modify):
- `ios/CLAUDE.md`
- `ios/AGENTS.md`
- `android/AGENTS.md`
- `android/README.md`

**In scope** (create):
- `README.md` (repo root)
- `.env.example` (repo root)

**Out of scope** (do NOT touch):
- The coding-convention sections of `ios/CLAUDE.md` (Swift/SwiftUI rules, module
  boundaries, DO NOT list) — those are still accurate; only fix command/path drift.
- Any actual `package.json` scripts — this is a docs plan; do not invent new
  scripts. (If you believe a unifying root `package.json` is warranted, that is a
  separate decision — see STOP conditions.)
- Secret values: `.env.example` lists variable NAMES with placeholder values only,
  never real keys.

## Git workflow

- Branch: `advisor/004-docs-drift`
- Commit style: `docs: align developer docs with standalone repo layout`
- Do NOT push or open a PR unless instructed.

## Steps

### Step 1: Fix `ios/CLAUDE.md`

- Replace the "From repo root" block (lines 78-83) with the per-directory form:
  ```bash
  # Run from ios/
  pnpm run generate     # xcodegen generate
  pnpm run test:packages
  pnpm run test:app
  pnpm run test
  ```
  Remove the `pnpm run ios:generate` / `pnpm run ios:test` lines (no root workspace).
- Line 106: replace the design-token instruction with: "Design tokens are
  generated from the external `@cypress-ink-labs/design-system` package and synced
  by the `sync-tokens` GitHub Actions workflow. Do not hand-edit `Tokens.swift`;
  token changes are made upstream in the design-system package."
- Line 134: replace with an honest note: "Local Supabase setup (`supabase start`,
  `scripts/setup-local.sh`) lived in the original monorepo and is not part of this
  standalone repo. Use the `FamilyEvents-Cloud` scheme (hosted dev Supabase) for
  local development, or obtain the local-stack scripts from the team."
- Replace `apps/ios/` path prefixes in the project-structure block with `ios/`.

**Verify**: `grep -n "ios:generate\|ios:test\|pnpm --filter\|packages/design-system\|db:start\|apps/ios" ios/CLAUDE.md` → no matches.

### Step 2: Fix `ios/AGENTS.md`

- Replace the "Run from repo root" commands (lines 13-18) with the `ios/` forms
  from Step 1.
- Replace the token block (lines 42-50) with the same external-sync note.
- Replace the `verify:*` section (lines 52-65) with: "Verify iOS changes from
  `ios/`: `pnpm run test`. There is no cross-platform `verify:*` aggregate in this
  standalone repo; run each platform's tests directly."
- Replace `apps/ios/` path references with `ios/`.

**Verify**: `grep -n "ios:generate\|ios:test\|verify:\|pnpm --filter\|packages/design-system\|apps/ios" ios/AGENTS.md` → no matches.

### Step 3: Fix `android/AGENTS.md` and `android/README.md`

In both files:
- Replace `pnpm --filter @family-events/android check|test|build|lint` with the
  local forms: run from `android/` either `pnpm run check|test|build|lint` (these
  wrap the env script) or `./gradlew check|test|assembleDebug|lint`.
- Replace the token-edit block with the external-sync note (as Step 1).
- In `android/AGENTS.md`, replace the `verify:android` / `verify:web` section with:
  "Verify Android changes from `android/`: `pnpm run check`. No cross-platform
  `verify:*` aggregate exists in this standalone repo."

**Verify**: `grep -rn "pnpm --filter\|verify:android\|verify:web\|packages/design-system" android/AGENTS.md android/README.md` → no matches.

### Step 4: Create the root `README.md`

Write `/README.md`:

```markdown
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
```

**Verify**: `ls README.md` → listed; `grep -c "Quick start" README.md` → 1.

### Step 5: Create `.env.example`

Gather the variable NAMES (not values) from `android/app/build.gradle.kts`
(`getEnvValue("...")` calls) and `ios/project.yml` (the build-config keys). Write
`/.env.example` with placeholders and comments — NEVER real keys:

```bash
# Copy to .env and fill in. .env is git-ignored. Do NOT commit real values.

# Supabase (publishable anon key — safe to ship in clients; RLS enforces access)
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=sb_publishable_xxx           # Supabase dashboard → API

# Maps (Android)
MAP_STYLE_URL=https://demotiles.maplibre.org/style.json

# Google Sign-In
ANDROID_GOOGLE_WEB_CLIENT_ID=xxx.apps.googleusercontent.com   # Google Cloud Console
IOS_GOOGLE_CLIENT_ID=xxx.apps.googleusercontent.com

# Firebase Cloud Messaging (Android push) — from google-services / Firebase console
ANDROID_FIREBASE_APPLICATION_ID=
ANDROID_FIREBASE_API_KEY=
ANDROID_FIREBASE_PROJECT_ID=
ANDROID_FIREBASE_GCM_SENDER_ID=
```

**Verify**: `ls .env.example` → listed. Then confirm NO real values were copied
in: open `ios/project.yml` and `android/app/build.gradle.kts`, take the actual
`SUPABASE_ANON_KEY` value and the production Supabase project ref from those
files, and `grep` each against `.env.example` — both must return zero matches.
`.env.example` must contain only placeholder values (e.g. `sb_publishable_xxx`,
`https://your-project.supabase.co`).

## Test plan

- Docs/markdown have no automated tests. Verification = the grep checks per step
  plus a read-through confirming the rewritten commands match `ios/package.json`
  and `android/package.json` exactly.
- Spot-run one rewritten command to prove it works: `cd ios && pnpm run generate`
  should exit 0 (requires xcodegen + node). If xcodegen is unavailable in the
  executor environment, skip and note it — do not "fix" by reverting the docs.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -rn "pnpm --filter\|pnpm run ios:\|pnpm run android:\|verify:ios\|verify:web\|verify:android\|packages/design-system\|pnpm run db:start" ios/CLAUDE.md ios/AGENTS.md android/AGENTS.md android/README.md` returns no matches
- [ ] `grep -rn "apps/ios\|apps/android" ios/CLAUDE.md ios/AGENTS.md android/AGENTS.md android/README.md` returns no matches
- [ ] `README.md` and `.env.example` exist at repo root
- [ ] `.env.example` contains NO real key values or production project refs — only placeholders (verified by grepping the actual values from `ios/project.yml` / `android/app/build.gradle.kts` against `.env.example` and getting zero matches)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- You conclude the docs genuinely require a new root `package.json` / workspace to
  describe accurately. Re-introducing a workspace contradicts the extraction
  intent — report the tradeoff and let a human decide; do not create one here.
- The local-Supabase scripts (`scripts/setup-local.sh`) turn out to exist
  somewhere reachable — then document the real path instead of the "not in this
  repo" note.
- `ios/package.json` or `android/package.json` scripts differ from the Current
  state excerpts (they drifted) — re-read them and document what actually exists.

## Maintenance notes

- Keep `.env.example` in sync with `getEnvValue(...)` keys in
  `android/app/build.gradle.kts` and the build-config keys in `ios/project.yml`.
  Plan 005 touches the Android env path; re-check after it lands.
- A reviewer should verify every command in the docs by running it once.
- The publishable Supabase keys and Google client IDs currently committed in
  `ios/project.yml` / `android` build config are public-by-design for mobile
  clients (they ship in the binary); this plan does not remove them. If the team
  later decides to externalize them, update `.env.example` accordingly.
