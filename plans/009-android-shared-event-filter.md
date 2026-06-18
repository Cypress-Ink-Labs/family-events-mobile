# Plan 009 (revised): Extract a shared Android event-query predicate — reconciling the divergent filter chains

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving on. If a
> STOP condition occurs, stop and report. When done, update the status row for
> plan 009 in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 39dcd14..HEAD -- android/data`
> If any in-scope file changed since this plan was written, compare against the
> "Current state" excerpts before proceeding; on a mismatch, STOP.

## Status

- **Priority**: P3
- **Effort**: M
- **Risk**: MED (production filtering must not change; the in-memory test double
  intentionally gains two filters — this must be verified, not assumed)
- **Depends on**: none
- **Category**: tech-debt
- **Planned at**: commit `39dcd14`, 2026-06-18
- **Supersedes**: the original plan 009 (which assumed the two repos applied
  identical filters — they do NOT; that STOP condition is resolved here).

## Why this matters — and what the first attempt found

The event-list filter is implemented twice (in-memory test double + Room-backed
production repo). The original plan assumed both applied the same 7 per-row
filters and proposed unifying them. The executor correctly STOPPED: the chains are
**not** identical, so a naive single predicate would silently change behavior.
This revision reconciles them deliberately.

Divergence (verified):
- `RoomBackedEventRepository.observeEventList` applies **8 per-row filters** —
  `search, tagIds, dateKey, ageMin, ageMax, isFree, dateFrom, dateTo` — filters
  `cityId` at the DAO level (`eventDao.observeEvents(query.cityId?…)`), and has an
  `.ifEmpty { seedEvents()… }` fallback before filtering.
- `InMemoryEventRepository.observeEventList` applies **7 per-row filters** —
  per-row `cityId` plus `search, ageMin, ageMax, isFree, dateFrom, dateTo`. It does
  **not** filter `tagIds` or `dateKey`.

**Decision (canonical = production):** the shared predicate covers the per-row
SEMANTIC filters both repos should apply — `search, tagIds, dateKey, ageMin,
ageMax, isFree, dateFrom, dateTo` — and **excludes `cityId`** (the two repos
legitimately differ on *where* cityId is applied: Room at the DAO, in-memory
per-row; that stays untouched in each). Consequence: **production (Room) behavior
is unchanged**; the **in-memory double gains `tagIds` + `dateKey` filtering**,
aligning the test double with production semantics. This is a deliberate,
documented behavior change to the *test double only* — it must be confirmed not to
break existing `:data` tests.

## Current state

`android/data/src/main/java/com/familyevents/data/RoomBackedRepositories.kt`
(`observeEventList`, ~lines 116-131):

```kotlin
override fun observeEventList(query: EventQuery): Flow<List<EventDto>> =
    eventDao.observeEvents(query.cityId?.rawValue, limit = 250, offset = 0)
        .map { rows ->
            rows.map { it.toDto() }
                .ifEmpty { seedEvents().filterByCity(query.cityId).ifEmpty { seedEvents() } }
                .filter { event -> query.search.isNullOrBlank() || event.title.contains(query.search, ignoreCase = true) }
                .filter { event -> query.tagIds.isEmpty() || event.tags.any { it.id in query.tagIds } }
                .filter { event -> query.dateKey == null || event.startsAt.toString().startsWith(query.dateKey) }
                .filter { event -> query.ageMin == null || (event.ageMax ?: Int.MAX_VALUE) >= query.ageMin }
                .filter { event -> query.ageMax == null || (event.ageMin ?: 0) <= query.ageMax }
                .filter { event -> query.isFree == null || event.isFree == query.isFree }
                .filter { event -> query.dateFrom == null || !event.startsAt.isBefore(query.dateFrom) }
                .filter { event -> query.dateTo == null || !event.startsAt.isAfter(query.dateTo) }
                .drop(query.offset)
                .take(query.limit)
        }
```

`android/data/src/main/java/com/familyevents/data/InMemoryRepositories.kt`
(`observeEventList`, ~lines 129-141):

```kotlin
override fun observeEventList(query: EventQuery): Flow<List<EventDto>> =
    events.map { rows ->
        rows.filter { query.cityId == null || it.cityId == query.cityId }
            .filter { query.search.isNullOrBlank() || it.title.contains(query.search, ignoreCase = true) }
            .filter { query.ageMin == null || (it.ageMax ?: Int.MAX_VALUE) >= query.ageMin }
            .filter { query.ageMax == null || (it.ageMin ?: 0) <= query.ageMax }
            .filter { query.isFree == null || it.isFree == query.isFree }
            .filter { query.dateFrom == null || !it.startsAt.isBefore(query.dateFrom) }
            .filter { query.dateTo == null || !it.startsAt.isAfter(query.dateTo) }
            .drop(query.offset)
            .take(query.limit)
    }
```

`EventQuery` (Models.kt:49+) defines `cityId, search, tagIds: List<String>,
dateKey: String?, ageMin, ageMax, isFree, dateFrom, dateTo, offset, limit`.
`EventDto` exposes `tags` (each with `.id`), `title`, `ageMin/ageMax`, `isFree`,
`startsAt`, `cityId`.

## Commands you will need

| Purpose | Command | Expected |
|---------|---------|----------|
| Compile | `cd android && scripts/with-android-env.sh ./gradlew :data:compileDebugKotlin` | BUILD SUCCESSFUL |
| Test | `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest` | BUILD SUCCESSFUL |

## Scope

**In scope**:
- `android/data/src/main/java/com/familyevents/data/EventQueryFilter.kt` (create)
- `android/data/src/main/java/com/familyevents/data/RoomBackedRepositories.kt` (`observeEventList` only)
- `android/data/src/main/java/com/familyevents/data/InMemoryRepositories.kt` (`observeEventList` only)
- `android/data/src/test/java/com/familyevents/data/EventQueryFilterTest.kt` (create)

**Out of scope**:
- `cityId` handling — leave Room's DAO-level call and InMemory's per-row cityId line exactly as they are. `cityId` is NOT in the shared predicate.
- The `.ifEmpty { seedEvents()… }` fallback in Room, and `.drop/.take` paging in both — leave as-is.
- `EventQuery` / `EventDto` definitions.

## Git workflow

- Branch: `advisor/009-android-shared-event-filter` (force-create with `git switch -C`)
- Commit style: `refactor(android): extract shared EventDto.matchesQuery (excl. cityId)`

## Steps

### Step 1: Create the shared predicate (excludes cityId)

`android/data/src/main/java/com/familyevents/data/EventQueryFilter.kt`:

```kotlin
package com.familyevents.data

/** Per-row event-list filtering shared by the in-memory and Room-backed repos.
 *  EXCLUDES cityId on purpose: Room filters cityId at the DAO level and the
 *  in-memory repo filters it per-row, so each keeps its own cityId handling. */
internal fun EventDto.matchesQuery(query: EventQuery): Boolean =
    (query.search.isNullOrBlank() || title.contains(query.search, ignoreCase = true)) &&
    (query.tagIds.isEmpty() || tags.any { it.id in query.tagIds }) &&
    (query.dateKey == null || startsAt.toString().startsWith(query.dateKey)) &&
    (query.ageMin == null || (ageMax ?: Int.MAX_VALUE) >= query.ageMin) &&
    (query.ageMax == null || (ageMin ?: 0) <= query.ageMax) &&
    (query.isFree == null || isFree == query.isFree) &&
    (query.dateFrom == null || !startsAt.isBefore(query.dateFrom)) &&
    (query.dateTo == null || !startsAt.isAfter(query.dateTo))
```

Verify exact property/method names against `EventDto` before compiling.

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:compileDebugKotlin` → BUILD SUCCESSFUL.

### Step 2: Use it in Room (production behavior unchanged)

Replace Room's 8 per-row `.filter {}` calls with a single `.filter { it.matchesQuery(query) }`,
keeping `eventDao.observeEvents(query.cityId?…)`, the `.ifEmpty { … }` fallback,
and `.drop/.take`:

```kotlin
                rows.map { it.toDto() }
                    .ifEmpty { seedEvents().filterByCity(query.cityId).ifEmpty { seedEvents() } }
                    .filter { it.matchesQuery(query) }
                    .drop(query.offset)
                    .take(query.limit)
```

This is behavior-preserving for Room (same 8 filters, same order of effect).

### Step 3: Use it in InMemory (gains tagIds + dateKey — intentional)

Keep the per-row cityId line, then replace the remaining filters with the shared predicate:

```kotlin
        rows.filter { query.cityId == null || it.cityId == query.cityId }
            .filter { it.matchesQuery(query) }
            .drop(query.offset)
            .take(query.limit)
```

This ADDS `tagIds` + `dateKey` filtering to the in-memory double (it had neither),
aligning it with production. That is the intended reconciliation.

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:compileDebugKotlin` → BUILD SUCCESSFUL.

### Step 4: Pin behavior with a unit test

Create `EventQueryFilterTest.kt` (JUnit 4) building `EventDto` fixtures (model the
constructor after `seedEvents()` in `InMemoryRepositories.kt`). Assert
`matchesQuery` for: empty query matches all; search case-insensitive; `tagIds`
match/non-match; `dateKey` prefix match; ageMin/ageMax overlap including boundary;
isFree true/false; dateFrom/dateTo inclusive boundaries. (cityId is NOT tested here
— it's not part of the predicate.)

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest` → BUILD SUCCESSFUL, new tests pass.

### Step 5: Confirm no existing test regressed

**Verify**: the full `:data:testDebugUnitTest` run is green. If any PRE-EXISTING
in-memory-repo test now fails because the double newly filters `tagIds`/`dateKey`,
STOP and report it — that test encodes an expectation about the old under-filtering
and a human must decide whether to update the test or revisit the reconciliation.

## Done criteria

ALL must hold:

- [ ] `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest` → BUILD SUCCESSFUL, new tests pass
- [ ] `matchesQuery` exists once and is called by both repos' `observeEventList`
- [ ] Room's `observeEventList` keeps DAO-level cityId + `.ifEmpty` fallback; InMemory keeps its per-row cityId line
- [ ] `cityId` does NOT appear in `matchesQuery`
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 009 updated

## STOP conditions

Stop and report if:

- A pre-existing `:data` test fails due to the in-memory double's new tagIds/dateKey filtering (Step 5).
- `EventDto`/`EventQuery` field names differ from the excerpts.
- Room's per-row filter set differs from the 8 documented here (the file drifted).

## Maintenance notes

- `matchesQuery` is now the one place to change per-row list-filter rules. cityId
  is deliberately outside it — if cityId handling is ever unified, that is a
  separate change touching the DAO query.
- Reviewer: diff `matchesQuery` against Room's original 8 filters character-by-
  character to confirm production semantics are preserved, and confirm the
  in-memory double's new filtering is the intended alignment.
