# Plan 001: Repair the node guard tests and run them in CI (restore verification baseline)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- tests/ .github/`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: tests / dx
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

This repo was extracted from a monorepo where everything lived under `apps/ios/`
and `apps/android/`. After extraction the code moved to `ios/` and `android/` at
the repo root, but the node "guard" tests still resolve `apps/ios` / `apps/android`.
Result: the guards **do not work**. Two of them error out (paths don't exist),
and one (`ios-token-adoption`) passes *vacuously* because it silently skips
missing directories — a false green. These guards are the project's only
language-agnostic, no-Xcode verification that the consumer-only scope and design-
token rules still hold. They are also not wired into any CI workflow, so nothing
runs them on a pull request. This plan makes them pass against the real layout
and adds a lightweight CI workflow to run them — establishing the verification
baseline every later plan depends on.

## Current state

- `tests/guards/ios-scope.test.mjs` — structural guard for iOS consumer scope.
  Line 6-10 build paths under `apps/ios`:
  ```js
  const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), "../..")
  const projectYmlPath = path.join(repoRoot, "apps", "ios", "project.yml")
  const pathPolicyPath = path.join(repoRoot, "apps", "ios", "Packages", "FECore", "Sources", "FECore", "ConsumerAPIPath.swift")
  const scopeTestPath = path.join(repoRoot, "apps", "ios", "Packages", "FECore", "Tests", "FECoreTests", "ConsumerAPIPathTests.swift")
  const iosPackagesRoot = path.join(repoRoot, "apps", "ios", "Packages")
  ```
  In this repo those files actually live at `ios/project.yml`, `ios/Packages/...`.
- `tests/guards/android-scope.test.mjs` — line 6-9:
  ```js
  const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), "../..")
  const androidRoot = path.join(repoRoot, "apps", "android")
  const settingsPath = path.join(androidRoot, "settings.gradle.kts")
  const pathPolicyPath = path.join(androidRoot, "core", "src", "main", "java", "com", "familyevents", "core", "ConsumerApiPath.kt")
  ```
  Real path: `android/settings.gradle.kts`, `android/core/...`.
- `tests/guards/ios-token-adoption.test.mjs` — line 7:
  ```js
  const packagesRoot = path.join(repoRoot, "apps", "ios", "Packages")
  ```
  Real path: `ios/Packages`. Because `walkSwiftFiles` (line 53-75) swallows
  missing dirs with `try/catch { continue }`, this test currently reports zero
  violations and passes even though it scans nothing — fixing the path will make
  it actually scan the consumer packages for the first time.
- Confirmed broken: `node --test tests/guards/*.test.mjs` currently fails with
  `Cannot find module '.../tests/guards'` on some node versions, and the
  individual tests assert on `existsSync(...apps/ios...)` which is false.
- There is **no** CI workflow that runs these guards. Existing workflows
  (`.github/workflows/`) are `mobile-release.yml`, `android-release.yml`,
  `codeql.yml`, `sync-tokens.yml`, `dependency-review.yml` — none invoke `node --test`.

The fix is a pure search-and-replace of `"apps", "ios"` → `"ios"` and
`"apps", "android"` → `"android"` in the three test files, plus one new CI file.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Run guards | `node --test tests/guards/*.test.mjs` | all tests pass, exit 0 |
| Node version | `node --version` | v18+ (repo uses node 24 in CI) |

## Scope

**In scope** (the only files you should modify):
- `tests/guards/ios-scope.test.mjs`
- `tests/guards/android-scope.test.mjs`
- `tests/guards/ios-token-adoption.test.mjs`
- `.github/workflows/verify.yml` (create)

**Out of scope** (do NOT touch):
- Any Swift or Kotlin source. If `ios-token-adoption` reports real violations
  after the path fix, that is a STOP condition (see below) — do not edit source
  to make it pass.
- The other workflow files (`mobile-release.yml`, `codeql.yml`, etc.) — they are
  handled by plans 002 and 003.

## Git workflow

- Branch: `advisor/001-fix-guard-tests`
- Commit style: Conventional Commits (repo uses `fix:`, `chore:`, `ci:` — see
  `git log`). Example: `fix(tests): point guard tests at standalone repo layout`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Fix the iOS scope guard paths

In `tests/guards/ios-scope.test.mjs`, replace every `path.join(repoRoot, "apps", "ios", ...)`
with `path.join(repoRoot, "ios", ...)`. There are 4 such constants (lines 7-10).
Remove the `"apps",` segment only; keep everything else identical.

**Verify**: `node --test tests/guards/*.test.mjsios-scope.test.mjs` → all tests pass, exit 0.

### Step 2: Fix the Android scope guard paths

In `tests/guards/android-scope.test.mjs`, change line 7
`const androidRoot = path.join(repoRoot, "apps", "android")` to
`const androidRoot = path.join(repoRoot, "android")`. That single constant feeds
all other paths, so no other edits are needed.

**Verify**: `node --test tests/guards/*.test.mjsandroid-scope.test.mjs` → all tests pass, exit 0.

### Step 3: Fix the iOS token-adoption guard path

In `tests/guards/ios-token-adoption.test.mjs`, change line 7
`const packagesRoot = path.join(repoRoot, "apps", "ios", "Packages")` to
`const packagesRoot = path.join(repoRoot, "ios", "Packages")`.

**Verify**: `node --test tests/guards/*.test.mjsios-token-adoption.test.mjs` → test passes,
exit 0. If it now FAILS with a list of "non-token color usage(s)", that is a
real pre-existing violation surfaced for the first time — STOP (see STOP conditions).

### Step 4: Add a CI workflow that runs the guards

Create `.github/workflows/verify.yml`:

```yaml
name: verify

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

permissions:
  contents: read

jobs:
  guards:
    name: scope guards
    runs-on: ubuntu-latest
    timeout-minutes: 5
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4
        with:
          persist-credentials: false
      - name: Setup Node
        uses: actions/setup-node@v4
        with:
          node-version: "24"
      - name: Run guard tests
        run: node --test tests/guards/*.test.mjs
```

**Verify**: file exists and is valid YAML —
`node -e "require('js-yaml')" 2>/dev/null || true` is optional; instead run
`cat .github/workflows/verify.yml` and confirm it matches the block above
(2-space indentation, no tabs).

### Step 5: Full guard run

**Verify**: `node --test tests/guards/*.test.mjs` → all tests across all three files pass,
exit 0.

## Test plan

- No new unit tests to author — this plan repairs existing tests.
- The added `verify.yml` is the regression guard: it ensures the path drift
  cannot silently return.
- Verification: `node --test tests/guards/*.test.mjs` → all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `node --test tests/guards/*.test.mjs` exits 0 with all tests passing
- [ ] `grep -rn "\"apps\"" tests/guards/` returns no matches
- [ ] `.github/workflows/verify.yml` exists and runs `node --test tests/guards/*.test.mjs`
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- After Step 3, `ios-token-adoption.test.mjs` fails with real color-token
  violations. That means consumer packages use raw SwiftUI colors — a separate
  fix. Report the violation list; do NOT edit Swift source to silence it.
- Any of the referenced files (`ios/project.yml`, `android/settings.gradle.kts`,
  `ios/Packages/FECore/Sources/FECore/ConsumerAPIPath.swift`,
  `android/core/.../ConsumerApiPath.kt`) does not exist at the standalone path —
  the layout differs from what this plan assumes.
- A guard assertion fails for a reason other than a path (e.g. an admin path is
  genuinely present) — that is a real scope violation to report, not to patch.

## Maintenance notes

- These guards are the cheapest signal that the consumer-only contract and the
  design-token rules still hold; keep `verify.yml` running on every PR.
- If the repo is ever re-nested under `apps/` again, all four files must change
  together.
- Reviewer should confirm the token-adoption guard actually scanned files this
  time (it was a silent no-op before) — a quick way is to temporarily introduce
  a `Color.accentColor` in a consumer package locally and confirm the guard
  fails, then revert.
