# Plan 005: Fix the broken `.env` fallback path in the Android Gradle build

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- android/app/build.gradle.kts`
> If the file changed since this plan was written, compare the "Current state"
> excerpt against the live code before proceeding; on a mismatch, STOP.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: dx / bug
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

The Android build reads configuration (`SUPABASE_URL`, keys, etc.) from three
sources in order: environment variable, Gradle `-P` property, then a root `.env`
file. The `.env` lookup uses a hard-coded relative path that assumed the monorepo
layout (`apps/android/` → repo root was two levels up). In the standalone repo the
Android project root is `android/`, so `../../.env` now points to the **parent of
the repository** — a path outside the repo that will essentially never hold the
project's `.env`. The `.env` fallback is therefore dead: it silently never
contributes a value. Env vars and `-P` still work, so builds aren't broken, but a
contributor who follows the documented "put values in root `.env`" flow gets no
effect and no error. This plan retargets the path to the actual repo-root `.env`.

## Current state

`android/app/build.gradle.kts`, the `getEnvValue` function (lines 10-31):

```kotlin
fun getEnvValue(key: String, allowViteFallback: Boolean = true): String? {
    // 1. System environment variable
    val env = System.getenv(key) ?: if (allowViteFallback) System.getenv("VITE_$key") else null
    if (!env.isNullOrBlank()) return env

    // 2. Project property (-Pkey=value)
    val prop = project.findProperty(key) as? String
        ?: if (allowViteFallback) project.findProperty("VITE_$key") as? String else null
    if (!prop.isNullOrBlank()) return prop

    // 3. .env file in root
    val envFile = project.rootProject.file("../../.env")   // <-- line 21: monorepo-era path
    if (envFile.exists()) {
        ...
    }
    return null
}
```

`project.rootProject` is the Gradle root project, whose directory is the
`android/` folder (where `settings.gradle.kts` lives). In the standalone repo the
shared `.env` sits one level up at the repository root, i.e. `android/../.env`.
So the correct relative path is `../.env`, not `../../.env`.

Confirmed: repo root holds `.env` (git-ignored, currently empty); there is no
`apps/` nesting. The comment on line 20 even says "`.env` file in root".

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Confirm repo-root .env location | `ls .env` (from repo root) | listed (file exists) |
| Android compiles config | `cd android && scripts/with-android-env.sh ./gradlew :app:help -q` | exit 0 (build script evaluates) |
| Find stale path | `grep -n "\.\./\.\./\.env" android/app/build.gradle.kts` | no matches after fix |

## Scope

**In scope**:
- `android/app/build.gradle.kts` — only the `.env` path on line 21.

**Out of scope** (do NOT touch):
- The env-var and `-P` lookup logic (sources 1 and 2) — they work correctly.
- The `VITE_` fallback behavior, the signing config, build types, or any
  `buildConfigField` — leave all unchanged.
- Any hardcoded default values (e.g. the debug Supabase key) — those are a
  separate, deliberately-deferred concern (publishable key, ships in clients).

## Git workflow

- Branch: `advisor/005-android-env-path`
- Commit style: `fix(android): point .env fallback at standalone repo root`
- Do NOT push unless instructed.

## Steps

### Step 1: Retarget the `.env` path

In `android/app/build.gradle.kts` line 21, change:
```kotlin
    val envFile = project.rootProject.file("../../.env")
```
to:
```kotlin
    val envFile = project.rootProject.file("../.env")
```

**Verify**: `grep -n "rootProject.file" android/app/build.gradle.kts` → shows
`"../.env"` and no `"../../.env"`.

### Step 2: Confirm the build script still evaluates

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :app:help -q`
→ exit 0 (this forces Gradle to evaluate `build.gradle.kts`, proving no syntax
error). If the env wrapper or SDK is unavailable in the executor environment,
instead confirm by reading the file that the only change is the path string.

## Test plan

- No unit test exercises Gradle config resolution directly. Manual verification:
  1. From repo root, create a temporary `.env` containing
     `SUPABASE_URL=https://example.test` (do NOT commit it; `.env` is git-ignored).
  2. `cd android && scripts/with-android-env.sh ./gradlew :app:assembleDebug -q`
     and confirm the build does not fail the release env-gate for a debug build
     (debug never trips the gate; this just proves the file is read without error).
  3. Delete the temporary `.env` contents afterward.
- This is optional if the Android SDK is unavailable; in that case rely on the
  Step 2 evaluation and the diff review.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -n "\.\./\.\./\.env" android/app/build.gradle.kts` returns no matches
- [ ] `grep -n "\"\.\./\.env\"" android/app/build.gradle.kts` returns exactly one match
- [ ] The diff changes ONLY the path string on line 21
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `git diff 744e1c6..HEAD -- android/app/build.gradle.kts` shows the function or
  path already changed (drift).
- `project.rootProject` is not the `android/` directory (e.g. a composite build
  was introduced) — then `../.env` may be wrong; report what `rootProject.projectDir`
  resolves to instead.

## Maintenance notes

- This path and `.env.example` (plan 004) describe the same repo-root `.env`; keep
  them consistent.
- A reviewer should confirm `../.env` resolves to the repository root given
  `settings.gradle.kts` lives in `android/`.
