# Plan 014: Complete plan 001 — fix 2 token violations, repair guard paths, add verify.yml

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on. If a
> STOP condition occurs, stop and report. When done, update the status rows for
> plans 001 and 014 in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 39dcd14..HEAD -- tests/guards ios/Packages/FEExplore .github/workflows`
> If any in-scope file changed since this plan was written, compare against the
> "Current state" excerpts before proceeding; on a mismatch, STOP.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none (supersedes the blocked plan 001)
- **Category**: tests / dx / bug
- **Planned at**: commit `39dcd14`, 2026-06-18

## Why this matters

Plan 001 was blocked on purpose: repairing the path drift in
`tests/guards/ios-token-adoption.test.mjs` made that guard actually scan the
consumer packages for the first time (it was a silent no-op pointing at a
nonexistent `apps/ios/Packages` dir), and it surfaced **2 real, pre-existing
design-token violations** in `ExploreFilterSheet.swift`. Plan 001 correctly
refused to edit Swift source (out of its scope) and stopped. This plan resolves
the blocker: fix the 2 violations (in scope here), apply 001's guard-path fixes,
and add the PR CI that runs the guards — so the verification baseline is finally
green and enforced.

## Current state

**Token violations** —
`ios/Packages/FEExplore/Sources/FEExplore/Components/ExploreFilterSheet.swift`:

```swift
// line 53
                                Text(label)
                                    .foregroundStyle(.primary)        // ← violation 1
                                Spacer()
                                if filters.activeCategory == slug {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(Color.accentColor)   // ← line 57, violation 2
                                }
```

The design-token helpers already exist and are the correct replacements
(confirmed in `ios/Packages/FEDesignSystem/Sources/FEDesignSystem/Color+Tokens.swift`):
`Color.dsTextPrimary` (line 47), `Color.dsAccentPrimary` (line 63). They are used
the same way elsewhere, e.g. `FEDesignSystem/EventCard.swift:63,70`.

**Guard path drift** (same as plan 001, still unmerged) — the three files under
`tests/guards/` resolve `apps/ios` / `apps/android` paths that don't exist in this
standalone repo:
- `tests/guards/ios-scope.test.mjs` lines 7-10: `path.join(repoRoot, "apps", "ios", ...)` ×4
- `tests/guards/android-scope.test.mjs` line 7: `path.join(repoRoot, "apps", "android")`
- `tests/guards/ios-token-adoption.test.mjs` line 7: `path.join(repoRoot, "apps", "ios", "Packages")`

**No PR CI** runs the guards (no `.github/workflows/verify.yml`).

Confirmed: `node --test tests/guards/*.test.mjs` currently FAILS (paths point at nonexistent
`apps/...`).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Run guards | `node --test tests/guards/*.test.mjs` | all tests pass, exit 0 |
| Confirm token helpers | `grep -n "dsTextPrimary\|dsAccentPrimary" ios/Packages/FEDesignSystem/Sources/FEDesignSystem/Color+Tokens.swift` | both present |

## Scope

**In scope** (modify):
- `ios/Packages/FEExplore/Sources/FEExplore/Components/ExploreFilterSheet.swift` (the 2 lines only)
- `tests/guards/ios-scope.test.mjs`, `tests/guards/android-scope.test.mjs`, `tests/guards/ios-token-adoption.test.mjs`
- `.github/workflows/verify.yml` (create)

**Out of scope** (do NOT touch):
- Any other Swift source, any other guard logic, any other workflow file.
- Do NOT change the design-token VALUES or `Color+Tokens.swift`.

## Git workflow

- Branch: `advisor/014-complete-001-token-fix-and-guards`
- Commit style: `fix: resolve token violations + repair scope guards + add verify CI`
- Do NOT push or open a PR.

## Steps

### Step 1: Fix the 2 token violations

In `ExploreFilterSheet.swift`:
- Line 53: `.foregroundStyle(.primary)` → `.foregroundStyle(Color.dsTextPrimary)`
- Line 57: `.foregroundStyle(Color.accentColor)` → `.foregroundStyle(Color.dsAccentPrimary)`

Ensure the file can resolve those symbols: it must `import FEDesignSystem`. Check
the top of the file; if the import is missing, add `import FEDesignSystem` (it is
a direct dependency of the FEExplore package, so this is in scope and safe).

**Verify**: `grep -n "dsTextPrimary\|dsAccentPrimary\|\.foregroundStyle(\.primary)\|Color\.accentColor" ios/Packages/FEExplore/Sources/FEExplore/Components/ExploreFilterSheet.swift`
→ shows the two new `ds…` usages and NO remaining `.foregroundStyle(.primary)` or `Color.accentColor`.

### Step 2: Repair the guard paths

Strip the `"apps", ` segment in all three guard files (identical to plan 001):
- `ios-scope.test.mjs`: 4 constants `path.join(repoRoot, "apps", "ios", …)` → `path.join(repoRoot, "ios", …)`.
- `android-scope.test.mjs` line 7: `path.join(repoRoot, "apps", "android")` → `path.join(repoRoot, "android")`.
- `ios-token-adoption.test.mjs` line 7: `path.join(repoRoot, "apps", "ios", "Packages")` → `path.join(repoRoot, "ios", "Packages")`.

**Verify**: `grep -rn "\"apps\"" tests/guards/` → no matches.

### Step 3: Confirm all guards pass (including token-adoption now)

**Verify**: `node --test tests/guards/*.test.mjs` → ALL tests pass, exit 0. The
`ios-token-adoption` guard must now pass (the 2 violations are fixed). If it still
reports a violation, STOP and report the exact list — do not chase additional
Swift edits beyond the 2 documented lines without reporting first.

### Step 4: Add the PR CI workflow

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

**Verify**: `cat .github/workflows/verify.yml` matches the block (2-space indent, no tabs).

## Test plan

- This repairs/enables existing guards; `verify.yml` is the regression guard.
- Verification: `node --test tests/guards/*.test.mjs` → all pass.

## Done criteria

ALL must hold:

- [ ] `node --test tests/guards/*.test.mjs` exits 0, all tests pass
- [ ] `grep -rn "\"apps\"" tests/guards/` → no matches
- [ ] `grep -n "\.foregroundStyle(\.primary)\|Color\.accentColor" ios/Packages/FEExplore/Sources/FEExplore/Components/ExploreFilterSheet.swift` → no matches
- [ ] `.github/workflows/verify.yml` exists and runs `node --test tests/guards/*.test.mjs`
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` rows for 001 and 014 updated

## STOP conditions

Stop and report if:

- The token-adoption guard still fails after the 2 documented fixes (there are
  more violations than the 2 known ones — report the list, don't keep editing).
- `Color.dsTextPrimary`/`Color.dsAccentPrimary` cannot be resolved (the FEDesignSystem
  import path differs from expectation).
- A guard fails for a non-path reason (a genuine scope/admin violation).

## Maintenance notes

- The Swift edit is verified only by the grep-based token guard here (this
  environment has no full Xcode to compile-check). A reviewer with Xcode should
  confirm `FEExplore` still builds — but the symbols are confirmed to exist in
  `FEDesignSystem` and are used identically elsewhere, so risk is low.
- Once this lands, plan 001's ticket (HEX-45) is fully satisfied; mark it Done.
