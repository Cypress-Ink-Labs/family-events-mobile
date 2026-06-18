# Plan 010: Split the two oversized iOS views into subviews (per repo guideline)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- ios/Packages/FEEventDetail ios/Packages/FEAuth`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P3
- **Effort**: M
- **Risk**: LOW (pure view extraction; snapshot/unit tests guard behavior)
- **Depends on**: none
- **Category**: tech-debt
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

`ios/CLAUDE.md` line 99 states the team convention: "Extract subviews when files
exceed ~150 lines or a view body exceeds ~80 lines." Two views violate it badly:
`EventDetailScreen.swift` (354 lines) and `ProfileSheet.swift` (300 lines). Large
view bodies are hard to review, hard to test in isolation, and a known churn
hotspot. Splitting them into focused subviews aligns with the documented rule and
makes each section independently snapshot-testable. This is a mechanical,
behavior-preserving refactor.

## Current state

`ios/Packages/FEEventDetail/Sources/FEEventDetail/EventDetailScreen.swift` — 354
lines. A `@MainActor public struct EventDetailScreen` whose `body` (from line 39)
switches on loading/error/loaded and, in the loaded `content(for:)` path, inlines
hero image, info grid, comment section, rating section, location, source, about,
tags. State held inline: `viewModel`, `calendarToast`, `draftComment`,
`draftRating`. It already factors some `@ViewBuilder` helpers, but the file is far
over the 150-line guideline.

`ios/Packages/FEAuth/Sources/FEAuth/Screens/ProfileSheet.swift` — 300 lines. A
`NavigationStack` switching on session state, wrapping a profile form, progress,
and notification-preferences UI with inline `@State` and task/dialog closures.

Both packages have tests under their `Tests/` dirs (e.g.
`FEEventDetail/Tests/...`, `FEAuth/Tests/...`) including ViewModel tests and
possibly snapshot tests.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| FEEventDetail tests | `cd ios/Packages/FEEventDetail && swift test` | all pass |
| FEAuth tests | `cd ios/Packages/FEAuth && swift test` | all pass |
| Line counts | `wc -l ios/Packages/FEEventDetail/Sources/FEEventDetail/EventDetailScreen.swift ios/Packages/FEAuth/Sources/FEAuth/Screens/ProfileSheet.swift` | each ≤ ~180 after split |

## Scope

**In scope**:
- `ios/Packages/FEEventDetail/Sources/FEEventDetail/EventDetailScreen.swift`
- New sibling files in `ios/Packages/FEEventDetail/Sources/FEEventDetail/` for the
  extracted subviews (e.g. `EventDetailSections.swift`).
- `ios/Packages/FEAuth/Sources/FEAuth/Screens/ProfileSheet.swift`
- New sibling files in `ios/Packages/FEAuth/Sources/FEAuth/Screens/` for the
  extracted subviews.

**Out of scope** (do NOT touch):
- The view models (`EventDetailViewModel`, `ProfileViewModel`) — logic unchanged.
- The public initializers / call sites of `EventDetailScreen` and `ProfileSheet`
  — their signatures must not change.
- Visual output — snapshots must pass without re-recording.
- Any other screen.

## Git workflow

- Branch: `advisor/010-ios-split-views`
- Commit style: `refactor(ios): extract subviews from EventDetail/Profile screens`
- One commit per screen is fine.
- Do NOT push unless instructed.

## Steps

### Step 1: Extract `EventDetailScreen` sections

Move the inlined section builders into standalone `private struct` subviews in a
new file `EventDetailSections.swift` (same module, so `internal`/`private`-to-file
access works — use `struct`s in the same module, not `private`). Suggested splits,
each taking the data/bindings it needs as `let`/`@Binding`:
- `EventDetailHero` (image + title)
- `EventDetailInfoGrid`
- `EventDetailCommentSection` (binds `draftComment`)
- `EventDetailRatingSection` (binds `draftRating`)
- `EventDetailLocationSection`, `EventDetailSourceSection`, `EventDetailAboutSection`

`EventDetailScreen.body` then composes these. Keep the `.task { ... }` /
`.onDisappear { ... }` lifecycle and all state on the parent `EventDetailScreen`.
Target: `EventDetailScreen.swift` ≤ ~150 lines.

**Verify**: `cd ios/Packages/FEEventDetail && swift test` → all pass (no snapshot
re-recording).

### Step 2: Extract `ProfileSheet` sections

Similarly split `ProfileSheet` into the form, the notification-preferences block,
and the loading/error states as sibling subviews in
`ios/Packages/FEAuth/Sources/FEAuth/Screens/`. Keep the `NavigationStack` and
session-state switch in `ProfileSheet`. Target: `ProfileSheet.swift` ≤ ~150 lines.

**Verify**: `cd ios/Packages/FEAuth && swift test` → all pass.

### Step 3: Confirm sizes and public API unchanged

**Verify**:
- `wc -l` (command above) shows both files materially smaller (≤ ~180).
- The `public init(...)` signatures of `EventDetailScreen` and `ProfileSheet` are
  unchanged (`git diff` shows no change to the init parameter lists).

## Test plan

- Behavior-preserving: existing package tests (ViewModel + any snapshot tests) are
  the regression guard and must pass without re-recording snapshots.
- Optionally add a snapshot test for one newly-extracted subview (e.g.
  `EventDetailInfoGrid`) if the package already has a snapshot-test pattern to
  copy; otherwise skip — do not introduce a new snapshot harness here.
- Verification: `swift test` in both packages → all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `cd ios/Packages/FEEventDetail && swift test` → all pass (no snapshot re-recording)
- [ ] `cd ios/Packages/FEAuth && swift test` → all pass
- [ ] `EventDetailScreen.swift` and `ProfileSheet.swift` are each ≤ ~180 lines (`wc -l`)
- [ ] Public `init` signatures of both screens unchanged (`git diff`)
- [ ] No files outside the in-scope list/new sibling files are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- A snapshot test fails or demands re-recording — the visual output changed.
- Extraction would require changing a public initializer or moving state into a
  child in a way that alters behavior.
- `swift test` cannot run in the executor environment — do not ship an unverified
  SwiftUI change.

## Maintenance notes

- This only addresses the two worst offenders. `RootView.swift` (306 lines) and
  `MapScreen.swift` (221) are next if the team wants to apply the guideline
  broadly — deferred.
- A reviewer should confirm zero behavior change (snapshots) and that the parent
  still owns all `@State` and lifecycle modifiers.
