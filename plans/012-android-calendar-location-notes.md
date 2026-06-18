# Plan 012: Add location + description to Android "Add to Calendar" (parity with iOS)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command. If anything in "STOP conditions" occurs, stop and report.
> When done, update the status row in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- android/platform`
> If in-scope files changed, compare excerpts against live code; on mismatch, STOP.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: direction (feature parity)
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

When a user adds an event to their calendar, iOS writes a rich entry — title,
start, end, timezone, **location, and notes** — via `EventKitWriter`. Android
sends an `ACTION_INSERT` intent with only the title and times; the venue and
description are dropped. Users on Android get a bare calendar entry and lose the
"where" and "what". The Android calendar intent supports both extras already
(`CalendarContract.Events.EVENT_LOCATION` and `.DESCRIPTION`), so this is a small,
self-contained parity fix.

## Current state

`android/platform/src/main/java/com/familyevents/platform/PlatformActions.kt`
(lines 23-31):

```kotlin
fun addToCalendar(title: String, startMillis: Long, endMillis: Long?) {
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, title)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (endMillis != null) intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
    context.startActivity(intent)
}
```

iOS reference (richer): `ios/Packages/FEEventDetail/Sources/FEEventDetail/EventKitWriter.swift`
populates title, start, end, timezone, location (venue name/address), and notes.

The Android call site(s) of `addToCalendar` pass title + times today; they must be
updated to also pass location + description (the event already carries
`venueName`/`address`/`description` fields — confirm via the event DTO).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Find call sites | `grep -rn "addToCalendar" android` | lists definition + all callers |
| Compile platform | `cd android && scripts/with-android-env.sh ./gradlew :platform:compileDebugKotlin` | BUILD SUCCESSFUL |
| Compile callers' module | `cd android && scripts/with-android-env.sh ./gradlew :app:compileDebugKotlin :eventdetail:compileDebugKotlin` | BUILD SUCCESSFUL |

## Scope

**In scope**:
- `android/platform/src/main/java/com/familyevents/platform/PlatformActions.kt`
- Each caller of `addToCalendar` (find with grep) — to pass the new arguments.

**Out of scope** (do NOT touch):
- `share()` / `directions()` in the same file.
- iOS code.
- The event data model — only READ `venueName`/`address`/`description` to pass them.

## Git workflow

- Branch: `advisor/012-android-calendar-fields`
- Commit style: `feat(android): include location and notes when adding to calendar`
- Do NOT push unless instructed.

## Steps

### Step 1: Widen `addToCalendar`

Add optional `location` and `description` parameters and put them as intent extras
when present:

```kotlin
fun addToCalendar(
    title: String,
    startMillis: Long,
    endMillis: Long?,
    location: String? = null,
    description: String? = null,
) {
    val intent = Intent(Intent.ACTION_INSERT)
        .setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.Events.TITLE, title)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (endMillis != null) intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
    if (!location.isNullOrBlank()) intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location)
    if (!description.isNullOrBlank()) intent.putExtra(CalendarContract.Events.DESCRIPTION, description)
    context.startActivity(intent)
}
```

The new params are optional with defaults, so existing callers still compile.

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :platform:compileDebugKotlin` → BUILD SUCCESSFUL.

### Step 2: Pass location + notes from each caller

`grep -rn "addToCalendar" android` to find every caller. For each, build a
`location` from the event's venue/address (e.g. `listOfNotNull(venueName, address).joinToString(", ")`)
and `description` from the event's description, and pass them. Match how iOS builds
its location/notes in `EventKitWriter.swift` for consistency.

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :app:compileDebugKotlin :eventdetail:compileDebugKotlin` (and any other module that calls it) → BUILD SUCCESSFUL.

## Test plan

- This is intent-construction glue with no pure logic worth a unit test unless you
  extract the location/description assembly into a helper — optional. If you do,
  add a small JUnit test asserting `listOfNotNull(venueName, address).joinToString(", ")`
  produces the expected string for (both present / one null / both null).
- Primary verification is compilation of the definition and all callers.
- Manual (operator, optional): on a device/emulator, tap "Add to calendar" and
  confirm the system calendar pre-fills location and notes.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `cd android && scripts/with-android-env.sh ./gradlew :platform:compileDebugKotlin :app:compileDebugKotlin :eventdetail:compileDebugKotlin` → BUILD SUCCESSFUL
- [ ] `addToCalendar` puts `EVENT_LOCATION` and `DESCRIPTION` extras when non-blank
- [ ] Every caller found by `grep -rn "addToCalendar" android` passes location + description
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- The event model exposed at a call site has no venue/address/description fields to
  pass — report what fields are available.
- A caller is in a module that doesn't already depend on `:platform` — wiring a new
  module dependency is out of scope; report it.

## Maintenance notes

- Keep the location/notes assembly consistent with iOS `EventKitWriter` so both
  platforms write the same calendar content.
- A reviewer should confirm extras are only added when non-blank (no empty
  "Location:" lines in the calendar entry).
