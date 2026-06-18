# Plan 008: Stop rebuilding event lookups on every SwiftUI body evaluation (iOS)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- ios/Packages/FESaved ios/Packages/FEPlan`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

Two list screens derive their content through chained computed properties that
each rebuild an event-lookup `Dictionary` from scratch. Because they are computed
properties (not stored), every SwiftUI `body` evaluation — which happens on every
filter change, every `@Query` update, and every parent re-render — runs an O(n)
dictionary construction, and on `SavedScreen` it runs it **multiple times per
body** (`filteredFavorites` → `resolvedFavorites` → `eventsByID`, each re-reading
`eventsByID`). This is wasted CPU on the main thread during scrolling and tab
switches. The fix is to build the lookup once per body and thread it through a
single pass — behavior-identical, just not repeated.

## Current state

`ios/Packages/FESaved/Sources/FESaved/Screens/SavedScreen.swift` (lines 30-46):

```swift
private var eventsByID: [String: CachedEvent] {
    Dictionary(uniqueKeysWithValues: cachedEvents.map { ($0.id, $0) })
}

private var resolvedFavorites: [(CachedFavorite, CachedEvent)] {
    favorites.compactMap { fav in
        guard fav.userID == userID.rawValue else { return nil }
        guard let event = eventsByID[fav.eventID] else { return nil }   // rebuilds eventsByID
        return (fav, event)
    }
}

private var filteredFavorites: [(CachedFavorite, CachedEvent)] {
    resolvedFavorites.filter { _, event in                              // rebuilds resolvedFavorites → eventsByID again
        filter.includes(eventStart: event.startDatetime)
    }
}
```

`ios/Packages/FEPlan/Sources/FEPlan/Screens/SaturdayPlanScreen.swift` (lines 26-39):

```swift
private var eventsByID: [String: CachedEvent] {
    Dictionary(uniqueKeysWithValues: cachedEvents.map { ($0.id, $0) })
}
private var heroEvent: EventDTO? {
    guard let first = cachedPlan.first, let cached = eventsByID[first.eventID] else { return nil }
    return cached.asEventDTO()
}
private var secondaryEvents: [EventDTO] {
    cachedPlan.dropFirst().prefix(6).compactMap { row in
        eventsByID[row.eventID]?.asEventDTO()                           // rebuilds eventsByID per element-ish access
    }
}
```

Both are `@MainActor` SwiftUI views backed by SwiftData `@Query` arrays
(`cachedEvents`, `favorites`, `cachedPlan`). The package has snapshot/unit tests
under `ios/Packages/FESaved/Tests` and `ios/Packages/FEPlan/Tests`.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| FESaved tests | `cd ios/Packages/FESaved && swift test` | all pass |
| FEPlan tests | `cd ios/Packages/FEPlan && swift test` | all pass |

(Per `ios/CLAUDE.md`, per-package `swift test` is the fast loop and does not need
full Xcode. If `swift test` cannot resolve dependencies in the executor
environment, STOP and report — do not push an unverified view change.)

## Scope

**In scope**:
- `ios/Packages/FESaved/Sources/FESaved/Screens/SavedScreen.swift`
- `ios/Packages/FEPlan/Sources/FEPlan/Screens/SaturdayPlanScreen.swift`

**Out of scope** (do NOT touch):
- The `@Query` declarations, the filter enum, `asEventDTO()`, or any model code.
- Visual output / view hierarchy — the rendered result must be byte-identical
  (snapshot tests must still pass with no re-recording).
- `ExploreCard` / `SafeImageURL` — image-URL memoization is a possible follow-up
  but is out of scope here (see Maintenance notes).

## Git workflow

- Branch: `advisor/008-ios-derivation-caching`
- Commit style: `perf(ios): build event lookup once per body in Saved/Plan screens`
- Do NOT push unless instructed.

## Steps

### Step 1: Single-pass derivation in `SavedScreen`

Convert the three computed properties into pure functions that take the lookup as
a parameter, and build the lookup once at the top of `body`. Replace lines 30-46
with helpers like:

```swift
private func eventsByID(_ events: [CachedEvent]) -> [String: CachedEvent] {
    Dictionary(events.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
}

private func filteredFavorites(index: [String: CachedEvent]) -> [(CachedFavorite, CachedEvent)] {
    favorites.compactMap { fav in
        guard fav.userID == userID.rawValue, let event = index[fav.eventID] else { return nil }
        return (fav, event)
    }
    .filter { _, event in filter.includes(eventStart: event.startDatetime) }
}
```

Then at the start of `body`:

```swift
public var body: some View {
    let index = eventsByID(cachedEvents)
    let rows = filteredFavorites(index: index)
    ScrollView { ... }   // use `rows` where `filteredFavorites` was referenced
}
```

Note the original used `Dictionary(uniqueKeysWithValues:)`, which crashes on
duplicate IDs; the replacement uses `uniquingKeysWith:` to be crash-safe while
keeping the first occurrence. If you want to preserve exact original semantics
instead, keep `uniqueKeysWithValues` — but `uniquingKeysWith` is strictly safer
and behavior-equivalent when IDs are unique (they are, being primary keys).

**Verify**: `cd ios/Packages/FESaved && swift test` → all pass.

### Step 2: Single-pass derivation in `SaturdayPlanScreen`

Apply the same shape: a `eventsByID(_:)` helper and `heroEvent(index:)` /
`secondaryEvents(index:)` functions, with `let index = eventsByID(cachedEvents)`
computed once at the top of `body` and threaded into both.

**Verify**: `cd ios/Packages/FEPlan && swift test` → all pass.

## Test plan

- These are behavior-preserving refactors; the existing package tests (including
  any snapshot tests) are the regression guard — they must pass WITHOUT
  re-recording snapshots. If a snapshot test fails, the output changed and the
  refactor is wrong — revert and report.
- Optionally add a tiny unit test in `FESavedTests` asserting that
  `filteredFavorites(index:)` filters by the selected `SavedFilter` for a known
  input set, modeled after an existing test in that target.
- Verification: `swift test` in both packages → all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `cd ios/Packages/FESaved && swift test` → all pass (no snapshot re-recording)
- [ ] `cd ios/Packages/FEPlan && swift test` → all pass
- [ ] `grep -n "private var eventsByID" ios/Packages/FESaved/Sources/FESaved/Screens/SavedScreen.swift ios/Packages/FEPlan/Sources/FEPlan/Screens/SaturdayPlanScreen.swift` → no matches (computed props replaced by functions)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- A snapshot test fails or demands re-recording — the visual output changed.
- `swift test` cannot run in the executor environment (missing toolchain/network) —
  do not ship an unverified SwiftUI change.
- The screens were already refactored (drift) or their structure differs from the
  Current state excerpts.

## Maintenance notes

- Follow-up deferred (separate plan if desired): `SafeImageURL.resolve(...)` is
  called inside `ExploreCard.body` (and Saved/EventDetail) per render. It is a
  pure function, so its result can be memoized (e.g. computed once when events are
  loaded and stored on the row view model) — a low-risk perf win not taken here.
- Reviewer should confirm the lookup is built exactly once per body and that no
  behavior changed (snapshots unchanged).
