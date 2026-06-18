# Plan 006: Log swallowed errors in Android repositories (observability)

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

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none (do plan 001 first so CI runs after)
- **Category**: correctness / observability
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

The Android data layer swallows every network/API failure and silently falls back
to seed (demo) data or an empty list. When the backend is down, slow, or returns
an error, the app shows fake events or no comments with **zero signal** — to the
user OR to a developer reading logs. A transient outage is indistinguishable from
"no data", which makes production issues nearly undebuggable. This plan keeps the
deliberate offline-fallback behavior (that is by design) but adds a log line at
each swallow site so failures are at least observable in `logcat` / crash tooling.

## Current state

`android/data/src/main/java/com/familyevents/data/RoomBackedRepositories.kt` —
the `RoomBacked*Repository` classes catch and discard exceptions, falling back to
`seedEvents()`:

```kotlin
// refreshPlan, lines ~136-140
val remotePlan = runCatching { api?.planEvents(userId, cityId, kidAge, lat, lng) }.getOrNull()
val events = remotePlan?.map { it.event }.takeUnless { it.isNullOrEmpty() }
    ?: runCatching { api?.events(EventQuery(cityId = cityId, limit = 50)) }.getOrNull().takeUnless { it.isNullOrEmpty() }
    ?: seedEvents().filterByCity(cityId)

// refreshEventList, lines ~157-159
val events = runCatching { api?.events(query) }.getOrNull().takeUnless { it.isNullOrEmpty() }
    ?: seedEvents().filterByCity(query.cityId)

// refreshEventDetail, lines ~163-165
(runCatching { api?.event(id) }.getOrNull() ?: seedEvents().firstOrNull { it.id == id })
    ?.let { eventDao.upsert(listOf(it.toEntity())) }
```

`android/data/src/main/java/com/familyevents/data/Repositories.kt` — the
`CommentRepository.observeComments` default polls and swallows with
`getOrDefault(emptyList())`:

```kotlin
// lines 84-89
fun observeComments(eventId: EventId): Flow<List<CommentDto>> = flow {
    while (true) {
        emit(runCatching { comments(eventId) }.getOrDefault(emptyList()))
        delay(CacheTtlTracker.COMMENTS_POLL_MS)
    }
}
```

There is no shared logging helper in `:data` today; Android's `android.util.Log`
is available (the `:data` module is an Android library). The `:auth` module
already uses `android.util.Log` with a `TAG` constant (see
`android/auth/.../AuthScreen.kt:6,43`) — match that idiom.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile + unit test data module | `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest` | BUILD SUCCESSFUL |
| Lint data module | `cd android && scripts/with-android-env.sh ./gradlew :data:lintDebug` | BUILD SUCCESSFUL |
| Find remaining bare swallows | `grep -rn "runCatching {" android/data/src/main/java/com/familyevents/data/RoomBackedRepositories.kt` | each followed by a log call |

## Scope

**In scope**:
- `android/data/src/main/java/com/familyevents/data/RoomBackedRepositories.kt`
- `android/data/src/main/java/com/familyevents/data/Repositories.kt`
- (Optional) a new small helper file
  `android/data/src/main/java/com/familyevents/data/RepoLogging.kt` if you prefer
  one shared function over inline logs.

**Out of scope** (do NOT touch):
- The fallback BEHAVIOR. Do not change what value is returned on failure — seed
  data / empty list must still be the fallback. Only ADD a log on the failure path.
- `InMemoryRepositories.kt` — those are test/demo doubles with no real API; no
  swallowing of real errors there.
- The `observeComments` polling cadence or the `while(true)` structure — the Flow
  is correctly cancellable (the `delay` is a cancellation point); do not "fix" it.

## Git workflow

- Branch: `advisor/006-log-android-swallows`
- Commit style: `fix(android): log swallowed API errors in repositories`
- Do NOT push unless instructed.

## Steps

### Step 1: Add a logging helper (recommended)

Create `android/data/src/main/java/com/familyevents/data/RepoLogging.kt`:

```kotlin
package com.familyevents.data

import android.util.Log

internal const val RepoLogTag = "FEData"

/** Runs [block]; on failure logs at WARN and returns null. Preserves the
 *  existing fallback semantics (caller treats null as "use fallback"). */
internal inline fun <T> repoOrNull(operation: String, block: () -> T): T? =
    runCatching(block).getOrElse { error ->
        Log.w(RepoLogTag, "API call failed: $operation — falling back", error)
        null
    }
```

If `Log` is not importable in `:data` (verify by compiling), instead add the
`Log.w(...)` calls inline at each site in Steps 2-3 without the helper.

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:compileDebugKotlin` → BUILD SUCCESSFUL.

### Step 2: Wrap the swallow sites in `RoomBackedRepositories.kt`

Replace each `runCatching { api?.foo(...) }.getOrNull()` with
`repoOrNull("foo") { api?.foo(...) }` (same returned value, now logged). Apply to:
- `refreshPlan`: the `planEvents(...)` and `events(...)` calls (~lines 137, 139).
- `refreshEventList`: the `events(query)` call (~line 158).
- `refreshEventDetail`: the `event(id)` call (~line 164).

Do not change the `?:` fallback chains or the `takeUnless { it.isNullOrEmpty() }`
logic — only the inner `runCatching{}.getOrNull()` becomes `repoOrNull("..."){}`.

**Verify**: `grep -n "runCatching {" android/data/src/main/java/com/familyevents/data/RoomBackedRepositories.kt`
→ no remaining bare `runCatching { api?` swallows (all replaced).

### Step 3: Log the comment-poll swallow in `Repositories.kt`

Change `observeComments` (lines 84-89) so the failure path logs but still emits an
empty list:

```kotlin
fun observeComments(eventId: EventId): Flow<List<CommentDto>> = flow {
    while (true) {
        emit(repoOrNull("comments(${eventId.rawValue})") { comments(eventId) } ?: emptyList())
        delay(CacheTtlTracker.COMMENTS_POLL_MS)
    }
}
```

(If `Repositories.kt` is a pure-Kotlin interface file with no Android imports and
`Log` won't resolve, keep the inline-helper approach but ensure the file can see
`repoOrNull`; it's in the same package so it will.)

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:compileDebugKotlin` → BUILD SUCCESSFUL.

### Step 4: Build + test the data module

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest`
→ BUILD SUCCESSFUL; existing data tests still pass.

## Test plan

- Add one unit test if a test source set exists for `:data`
  (`android/data/src/test/...`): construct a `RoomBacked*Repository` with an `api`
  stub that throws, call the refresh method, and assert it still returns/upserts
  the seed fallback (behavior unchanged). Logging itself is hard to assert in a
  unit test; the behavioral assertion (fallback still works) is the regression
  guard. Model the test after any existing test in `android/data/src/test/`.
- If no `:data` test source set exists, skip the new test and note it; the compile
  + existing-test run is the gate.
- Verification: `./gradlew :data:testDebugUnitTest` → all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest` → BUILD SUCCESSFUL
- [ ] `grep -rn "runCatching { api?" android/data/src/main/java/com/familyevents/data/RoomBackedRepositories.kt` → no matches (all wrapped)
- [ ] The returned fallback values are unchanged (diff shows only the swallow→logged-swallow substitution)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `android.util.Log` cannot be imported in `:data` AND there is no existing
  logging abstraction in `:core`/`:data` — report so a logging approach can be
  chosen rather than adding a dependency.
- Wrapping a site would change its return type or fallback value — that means the
  site is more than a simple swallow; report it.
- The data module's tests fail for a reason unrelated to your change.

## Maintenance notes

- This is observability, not behavior change — reviewers should confirm no
  fallback value changed.
- Follow-up deferred: surfacing these failures to the UI (a "couldn't refresh"
  banner) is a product decision, out of scope here.
- If a structured telemetry/crash SDK is later added, `repoOrNull` is the single
  chokepoint to route these warnings into it.
