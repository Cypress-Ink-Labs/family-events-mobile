# Plan 003: Repair CodeQL + dependency-review for the standalone layout and re-enable scanning

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- .github/workflows/`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: security / ci
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

CodeQL is the only static application security testing (SAST) on this repo. After
the monorepo extraction its path filters and build source-roots still reference
`apps/web`, `apps/android`, `apps/ios`, `packages/`, and `supabase/functions/` —
none of which exist here. Effects:
1. The PR trigger never fires (no changed file ever matches `apps/web/**` etc.),
   so CodeQL effectively **never runs on pull requests**.
2. Even when run on schedule/dispatch, the android and ios jobs `cd apps/android`
   / `source-root: apps/ios` and **fail** because those dirs are absent.
3. The `web` job scans for a web app that no longer lives in this repo.
`dependency-review.yml` has the same drift in its path filter.
This plan retargets scanning to the two languages this repo actually contains
(Kotlin, Swift), drops the dead web job, and re-enables the push trigger.

## Current state

`.github/workflows/codeql.yml`:
- Lines 5-16: the `push:` trigger is commented out, and its `paths:` list (and
  the active `pull_request.paths:` at lines 19-28) reference `apps/web/**`,
  `apps/android/**`, `packages/**`, `supabase/functions/**`, `pnpm-*.yaml`,
  `turbo.json` — none present in this repo.
- The `changes` job (lines 44-116) computes `web`/`android`/`ios` outputs from a
  `git diff` against patterns like `^apps/android/`. In this repo nothing matches,
  so on PRs `android`/`web` resolve to `false` and `ios` is never set true in the
  diff path.
- `web` job (lines 118-154): scans `source-root: .` for javascript-typescript —
  this repo has no web app (only the node guard tests in `tests/`).
- `android` job line 207: `source-root: apps/android`; line 210:
  `run: cd apps/android && scripts/with-android-env.sh ./gradlew ... assembleDebug`.
- `ios` job: line 259 + 267 cache keys hash `apps/ios/...`; line 277
  `source-root: apps/ios`; line 280 `run: cd apps/ios && xcodegen generate`;
  lines 284-293 `cd apps/ios` then `xcodebuild ... build`.

`.github/workflows/dependency-review.yml`:
- Line 12: `      - "apps/**/package.json"` — should be `"**/package.json"` to
  catch `ios/package.json` and `android/package.json`.

Correct standalone paths drop `apps/`: `android/`, `ios/`. The android build in
this repo uses the env wrapper `scripts/with-android-env.sh` (present at
`android/scripts/with-android-env.sh`).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Confirm dirs | `ls ios/project.yml android/scripts/with-android-env.sh` | both listed |
| Find stale refs | `grep -rn "apps/" .github/workflows/codeql.yml .github/workflows/dependency-review.yml` | no matches after fix |

(CodeQL itself cannot be run locally without the GitHub Actions runner; verify by
path-correctness and structural review.)

## Scope

**In scope**:
- `.github/workflows/codeql.yml`
- `.github/workflows/dependency-review.yml`

**Out of scope**:
- `mobile-release.yml`, `android-release.yml` — handled by plan 002.
- CodeQL query packs (`+security-extended,security-and-quality`) and action
  SHAs — leave the pinned versions exactly as they are.

## Git workflow

- Branch: `advisor/003-fix-codeql`
- Commit style: `fix(ci): retarget CodeQL + dependency-review to standalone layout`
- Do NOT push or trigger workflows unless instructed.

## Steps

### Step 1: Re-enable and retarget the CodeQL triggers

In `codeql.yml`:
- Uncomment the `push:` block (lines 5-16) and replace its `paths:` list with the
  standalone paths. Replace BOTH the push and the `pull_request.paths` (lines
  19-28) lists with:
  ```yaml
      paths:
        - "android/**"
        - "ios/**"
        - ".github/workflows/codeql.yml"
  ```
- In the `changes` job's `matches` logic (lines 100-116), replace the web/android
  detection. Since there is no web app, drop web detection. Set:
  ```bash
          android=false
          ios=false

          if matches '^android/' '^\.github/workflows/codeql\.yml$'; then
            android=true
          fi
          if matches '^ios/' '^\.github/workflows/codeql\.yml$'; then
            ios=true
          fi
  ```
  and remove the `web=` output line and the `web` output declaration (line 50).

**Verify**: `grep -n "apps/web\|apps/android\|apps/ios\|packages/\|supabase/functions\|turbo.json\|pnpm-" .github/workflows/codeql.yml` → no matches.

### Step 2: Remove the dead `web` job

Delete the entire `web:` job (lines 118-154 in the original) — this repo has no
web application for `javascript-typescript` analysis. (The node files under
`tests/guards/` are dev tests, not an app surface; scanning them adds noise.)
Remove the `web` entry from the `changes` job `outputs:` (line 50) if not already
removed in Step 1.

**Verify**: `grep -n "javascript-typescript" .github/workflows/codeql.yml` → no matches.

### Step 3: Retarget the `android` CodeQL job

- Line ~207: `source-root: android`
- Line ~210: `run: cd android && scripts/with-android-env.sh ./gradlew --no-build-cache --no-configuration-cache clean assembleDebug`

**Verify**: `grep -n "apps/android" .github/workflows/codeql.yml` → no matches.

### Step 4: Retarget the `ios` CodeQL job

Replace `apps/ios` with `ios` everywhere in the ios job:
- Cache-key `hashFiles(...)` args (lines ~259, ~267): `ios/project.yml`,
  `ios/Packages/**/Package.swift`, `ios/Packages/**/Package.resolved`,
  `ios/**/*.swift`.
- Line ~277: `source-root: ios`
- Line ~280: `run: cd ios && xcodegen generate`
- Lines ~284: `cd ios` (in the build step).

**Verify**: `grep -n "apps/ios" .github/workflows/codeql.yml` → no matches.

### Step 5: Fix dependency-review path filter

In `dependency-review.yml` line 12, change `"apps/**/package.json"` to
`"**/package.json"`.

**Verify**: `grep -n "apps/" .github/workflows/dependency-review.yml` → no matches.

### Step 6: Whole-file sanity

**Verify**: `grep -rn "apps/" .github/workflows/codeql.yml .github/workflows/dependency-review.yml`
→ no matches. Manually read both files to confirm YAML indentation is intact and
no unrelated step was altered.

## Test plan

- No unit tests apply.
- Verification is path-correctness (greps above) plus a manual read that the job
  graph (`changes` → `android` / `ios`) still wires `needs:` correctly after the
  `web` job removal.
- (Operator-gated) After merge a maintainer can run CodeQL via `workflow_dispatch`
  to confirm both jobs build and analyze. Do NOT trigger it yourself.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -rn "apps/" .github/workflows/codeql.yml .github/workflows/dependency-review.yml` returns no matches
- [ ] `grep -n "javascript-typescript" .github/workflows/codeql.yml` returns no matches (web job gone)
- [ ] `ls ios/project.yml android/scripts/with-android-env.sh` both succeed
- [ ] The `changes` job no longer declares or sets a `web` output, and `android`/`ios` jobs still reference `needs: changes`
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Removing the `web` job would orphan a `needs:` reference you cannot cleanly
  resolve.
- `ios/project.yml` or `android/scripts/with-android-env.sh` is missing.
- You are unsure whether dropping the web job is correct because you discover a
  web app surface in this repo (search `package.json` files first) — report
  instead of guessing.

## Maintenance notes

- After this lands, CodeQL runs on PRs touching `ios/**` or `android/**`. A
  reviewer should confirm the first post-merge scheduled run actually produces
  SARIF for both languages.
- If a web surface is ever added back, restore a `web` job rather than widening
  the android/ios jobs.
- Reviewer scrutiny: confirm no action SHA or query-pack string changed.
