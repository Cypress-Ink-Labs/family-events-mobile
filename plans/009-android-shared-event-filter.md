# Plan 009: Extract the duplicated Android event-query filter into one shared predicate

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- android/data/`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P3
- **Effort**: M
- **Risk**: MED (two code paths must stay behaviorally identical; a wrong
  extraction silently changes query results)
- **Depends on**: 001 (so the new test runs in CI)
- **Category**: tech-debt
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

The event-list query filter (search, age range, free, date range) is implemented
twice with the same chain of `.filter {}` calls: once in `InMemoryEventRepository`
(the demo/test double) and once in `RoomBackedEventRepository` (production). When
filtering rules change, both must be edited in lockstep, and they have already
diverged in style. Divergence here is dangerous because the in-memory repo backs
tests — if it filters differently from the Room repo, tests pass while production
behaves differently. This plan extracts a single `EventDto.matchesQuery(query)`
predicate that both repositories call, and adds a unit test pinning its behavior.

## Current state

`android/data/src/main/java/com/familyevents/data/InMemoryRepositories.kt`
(`InMemoryEventRepository.observeEventList`, lines 119-130):

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

`android/data/src/main/java/com/familyevents/data/RoomBackedRepositories.kt`
(`RoomBackedEventRepository.observeEventList`, lines ~115-131) has the SAME 7
filter conditions (verify by reading; line numbers approximate) followed by
`.drop(query.offset).take(query.limit)`, differing only in the upstream source
(`eventDao.observe...().map { ... }` vs the in-memory `MutableStateFlow`).

`EventDto` and `EventQuery` are defined in the `com.familyevents.data` package.
`EventDto` exposes `cityId`, `title`, `ageMin`, `ageMax`, `isFree`, `startsAt`
(a `java.time`/`kotlinx` instant supporting `isBefore`/`isAfter`).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile data | `cd android && scripts/with-android-env.sh ./gradlew :data:compileDebugKotlin` | BUILD SUCCESSFUL |
| Test data | `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest` | BUILD SUCCESSFUL |

## Scope

**In scope**:
- A new file `android/data/src/main/java/com/familyevents/data/EventQueryFilter.kt`
- `android/data/src/main/java/com/familyevents/data/InMemoryRepositories.kt`
- `android/data/src/main/java/com/familyevents/data/RoomBackedRepositories.kt`
- `android/data/src/test/java/com/familyevents/data/EventQueryFilterTest.kt` (create)

**Out of scope** (do NOT touch):
- `.drop(query.offset).take(query.limit)` paging — leave it in each repo's flow;
  only the per-row predicate is extracted.
- The data source (`MutableStateFlow` vs `eventDao`) — unchanged.
- `EventDto` / `EventQuery` definitions.

## Git workflow

- Branch: `advisor/009-android-event-filter`
- Commit style: `refactor(android): extract shared EventDto.matchesQuery predicate`
- Do NOT push unless instructed.

## Steps

### Step 1: Create the shared predicate

Create `android/data/src/main/java/com/familyevents/data/EventQueryFilter.kt`,
copying the EXACT 7 conditions from `InMemoryEventRepository` (the canonical form)
so behavior is preserved bit-for-bit:

```kotlin
package com.familyevents.data

/** Single source of truth for event-list filtering. Both the in-memory and
 *  Room-backed repositories MUST use this so test and production agree. */
internal fun EventDto.matchesQuery(query: EventQuery): Boolean =
    (query.cityId == null || cityId == query.cityId) &&
    (query.search.isNullOrBlank() || title.contains(query.search, ignoreCase = true)) &&
    (query.ageMin == null || (ageMax ?: Int.MAX_VALUE) >= query.ageMin) &&
    (query.ageMax == null || (ageMin ?: 0) <= query.ageMax) &&
    (query.isFree == null || isFree == query.isFree) &&
    (query.dateFrom == null || !startsAt.isBefore(query.dateFrom)) &&
    (query.dateTo == null || !startsAt.isAfter(query.dateTo))
```

Confirm the exact property/method names against `EventDto` before compiling.

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:compileDebugKotlin` → BUILD SUCCESSFUL.

### Step 2: Use it in `InMemoryEventRepository`

Replace the 7 chained `.filter {}` calls (lines 121-127) with a single
`.filter { it.matchesQuery(query) }`, keeping `.drop(query.offset).take(query.limit)`:

```kotlin
rows.filter { it.matchesQuery(query) }
    .drop(query.offset)
    .take(query.limit)
```

**Verify**: compile as above; `grep -c "\.filter {" InMemoryRepositories.kt` shows
the event-list block reduced to one filter.

### Step 3: Use it in `RoomBackedEventRepository`

Apply the same single-predicate replacement in `RoomBackedEventRepository.observeEventList`.

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:compileDebugKotlin` → BUILD SUCCESSFUL.

### Step 4: Pin behavior with a unit test

Create `android/data/src/test/java/com/familyevents/data/EventQueryFilterTest.kt`
(JUnit 4). Build a small set of `EventDto` fixtures and assert `matchesQuery`
for: empty query (matches all), city filter, search (case-insensitive),
age-overlap edges, `isFree`, and date bounds (inclusive on the boundary, per the
`!isBefore`/`!isAfter` semantics). Model fixture construction after the
`seedEvents()` shape in `InMemoryRepositories.kt` (same `EventDto` constructor).

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest` → BUILD SUCCESSFUL, new tests pass.

## Test plan

- New `EventQueryFilterTest.kt` covering: no-op query, city match/non-match,
  search case-insensitivity, ageMin/ageMax overlap including boundary values,
  isFree true/false, dateFrom/dateTo inclusive boundaries.
- Existing `:data` tests must still pass (they exercise the repos that now call
  the shared predicate) — that is the equivalence guard.
- Verification: `./gradlew :data:testDebugUnitTest` → all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest` → BUILD SUCCESSFUL with new passing tests
- [ ] `matchesQuery` exists once and is called by both `InMemoryEventRepository` and `RoomBackedEventRepository`
- [ ] Neither repo contains the 7-line `.filter{}` chain anymore (`grep` shows a single `matchesQuery` filter in each event-list flow)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The two repos' filter chains are NOT actually identical (e.g. one has an 8th
  condition or different boundary logic) — extracting a single predicate would
  change behavior. Report the divergence; do not pick one silently.
- `EventDto` property names differ from those assumed above.
- `:data` tests fail after the change for a filtering reason — that signals a
  behavior change; revert and report.

## Maintenance notes

- `matchesQuery` is now the single place to change list-filter rules; any new
  filter field on `EventQuery` is added here once.
- A reviewer should diff the predicate against the original 7 conditions
  character-by-character to confirm equivalence.
- Note: the Room repo filters in memory after a DAO read; pushing filters into SQL
  is a larger perf change explicitly deferred.
