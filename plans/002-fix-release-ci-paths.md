# Plan 002: Repair the release CI workflows for the standalone repo layout

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
- **Risk**: MED (these workflows produce App Store / Play Store builds; a wrong
  edit could publish a bad artifact — but they only run on manual dispatch or
  release tags, never on this change itself)
- **Depends on**: none
- **Category**: dx / ci
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

The repo was extracted from a monorepo. The two release workflows still `cd` into
`apps/android` and point `IOS_PROJECT` at `apps/ios/...`. Those directories do
not exist here (code is at `android/` and `ios/`). As written, **every release
run fails**: the iOS archive step cannot find the project, and the Android build
step `cd apps/android` exits non-zero. The release pipeline is silently dead.
This plan retargets the paths to the standalone layout so a release tag actually
produces a build.

## Current state

Two files reference the old layout.

`.github/workflows/mobile-release.yml`:
- Line 45: `  IOS_PROJECT: apps/ios/FamilyEvents.xcodeproj`
- Line 303: `          echo "$ANDROID_KEYSTORE_BASE64" | base64 --decode > apps/android/app/release.keystore`
- Line 318: `          cd apps/android`
- Line 326: `          releaseFiles: apps/android/app/build/outputs/bundle/release/*.aab`
- Lines 336-337 (artifact upload paths):
  ```yaml
            apps/android/app/build/outputs/bundle/release/*.aab
            apps/android/app/build/outputs/apk/release/*.apk
  ```

`.github/workflows/android-release.yml`:
- Line 52: `          echo "$ANDROID_KEYSTORE_BASE64" | base64 --decode > apps/android/app/release.keystore`
- Line 64: `          cd apps/android`
- Lines 76-78 (artifact upload paths):
  ```yaml
            apps/android/app/build/outputs/bundle/release/*.aab
            apps/android/app/build/outputs/apk/release/*.apk
            apps/android/app/build/outputs/apk/debug/*.apk
  ```

The correct standalone paths drop the `apps/` prefix: `ios/FamilyEvents.xcodeproj`,
`android/app/release.keystore`, `cd android`, `android/app/build/outputs/...`.

The Android build runs through gradle directly (`./gradlew :app:bundleRelease`)
from inside the android dir; the env wrapper `scripts/with-android-env.sh` is NOT
used here (CI sets JDK 17 explicitly), so leave the `./gradlew` invocation as-is —
only the directory changes.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Confirm dirs exist | `ls ios/FamilyEvents.xcodeproj android/app android/gradlew` | all listed, no error |
| Find stale refs | `grep -rn "apps/" .github/workflows/mobile-release.yml .github/workflows/android-release.yml` | no matches after fix |
| YAML sanity | `cat .github/workflows/mobile-release.yml` | inspect indentation manually |

(There is no way to fully exercise these workflows locally without release
secrets; verification is by static path-correctness plus a structural review.)

## Scope

**In scope**:
- `.github/workflows/mobile-release.yml`
- `.github/workflows/android-release.yml`

**Out of scope** (do NOT touch):
- `codeql.yml`, `dependency-review.yml` — handled by plan 003.
- Any signing logic, secret names, version-numbering logic, or step ordering —
  change ONLY the `apps/...` path segments. The signing flow is correct; do not
  "improve" it.
- The iOS `xcodebuild archive` flags — only `IOS_PROJECT` changes.

## Git workflow

- Branch: `advisor/002-fix-release-ci`
- Commit style: `fix(ci): retarget release workflows to standalone repo layout`
- Do NOT push, tag, or trigger a workflow unless the operator instructed it.

## Steps

### Step 1: Fix `mobile-release.yml` paths

Replace each occurrence listed in Current state, removing the `apps/` prefix:
- Line 45: `IOS_PROJECT: ios/FamilyEvents.xcodeproj`
- Line 303: `... base64 --decode > android/app/release.keystore`
- Line 318: `cd android`
- Line 326: `releaseFiles: android/app/build/outputs/bundle/release/*.aab`
- Lines 336-337: `android/app/build/outputs/bundle/release/*.aab` and
  `android/app/build/outputs/apk/release/*.apk`

Leave the surrounding script bodies, indentation, and the
`xcodebuild archive -project "$IOS_PROJECT" ...` step otherwise unchanged.

**Verify**: `grep -n "apps/" .github/workflows/mobile-release.yml` → no matches.

### Step 2: Fix `android-release.yml` paths

Replace:
- Line 52: `... base64 --decode > android/app/release.keystore`
- Line 64: `cd android`
- Lines 76-78: drop `apps/` from all three upload paths.

**Verify**: `grep -n "apps/" .github/workflows/android-release.yml` → no matches.

### Step 3: Confirm targets exist at the new paths

**Verify**: `ls ios/FamilyEvents.xcodeproj && ls android/app && ls android/gradlew`
→ all succeed (the workflows will `cd android` then call `./gradlew`).

## Test plan

- No unit tests apply to YAML workflows.
- Structural verification only: the grep checks above plus a manual read
  confirming no step body was altered other than the path segments.
- (Optional, operator-gated) After merge, a maintainer can run the
  `android-release` workflow via `workflow_dispatch` with `build_type: debug` to
  confirm the build step now succeeds end-to-end. Do NOT trigger this yourself.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -rn "apps/" .github/workflows/mobile-release.yml .github/workflows/android-release.yml` returns no matches
- [ ] `ls ios/FamilyEvents.xcodeproj android/app android/gradlew` all succeed
- [ ] `git diff` shows ONLY path-segment changes (no logic/secret/step changes)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `ios/FamilyEvents.xcodeproj` or `android/app` does not exist — the layout
  differs from this plan's assumption.
- You find additional `apps/` references in these two files beyond those listed
  in Current state (the files drifted since this plan was written).
- A fix appears to require changing a secret name, the gradle task list, or the
  signing config — that is out of scope; report instead.

## Maintenance notes

- If the repo is re-nested under `apps/` again, these paths and plan 001's guard
  tests must all change together.
- A reviewer should diff against the previous version and confirm every changed
  line is purely a path edit.
- Follow-up deferred: neither release workflow runs the unit tests before
  building. Wiring tests into the release gate is out of scope here (plan 001
  adds PR-time guards; a fuller pre-release test gate can come later).
