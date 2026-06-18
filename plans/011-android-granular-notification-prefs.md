# Plan 011: Bring Android notification preferences to parity with iOS (6 categories)

> **Executor instructions**: This is a **design + implementation** plan for a
> feature gap. Do the spike step (Step 1) FIRST and confirm the backend contract
> before writing UI. Run every verification command. If anything in "STOP
> conditions" occurs, stop and report. When done, update the status row in
> `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- android/ ios/Packages/FEData/Sources/FEData/Repositories/NotificationPreferencesRepo.swift`
> If in-scope files changed, compare excerpts against live code; on mismatch, STOP.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: MED (new user-facing surface + a backend RPC call; getting the RPC
  param names wrong silently no-ops the toggles)
- **Depends on**: 001 (CI), and best after 006 (repo logging) so failures are visible
- **Category**: direction (feature parity)
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

iOS users can control six notification categories (event reminders, event changes,
and weekly digest — each over email and push). Android users get **nothing**: the
Android `ProfileRepository` exposes only `updateNotificationPreference(userId, enabled: Boolean)`,
and the Android profile UI (`ProfileDialog.kt`) has no notification control at all.
The backend already supports the full six-field model (iOS calls the RPC
`upsert_notification_preferences` and reads `user_notification_preferences`), so
this is a client-only gap — no schema work. Closing it removes a real support-
friction asymmetry and is the highest-leverage parity win identified.

## Current state

**iOS contract (the source of truth to mirror)** —
`ios/Packages/FEData/Sources/FEData/Repositories/NotificationPreferencesRepo.swift`:

- Model `NotificationPreferences` with six `Bool`s: `reminderEmail`, `reminderPush`,
  `changeEmail`, `changePush`, `digestEmail`, `digestPush`. Defaults: all true
  except `digestPush = false`.
- `fetch(userID)`: selects columns
  `reminder_email,reminder_push,change_email,change_push,digest_email,digest_push`
  from table `user_notification_preferences` where `user_id == userID`, limit 1;
  falls back to `.defaults` when absent.
- `upsert(prefs, userID)`: calls RPC `upsert_notification_preferences` with params
  `p_reminder_email, p_reminder_push, p_change_email, p_change_push, p_digest_email, p_digest_push`
  (all `Bool`), returns the persisted row.

**Android gaps**:
- `android/data/.../Repositories.kt:35` — `ProfileRepository` has only
  `suspend fun updateNotificationPreference(userId: UserId, enabled: Boolean)`.
- `android/app/.../ProfileDialog.kt` — profile UI has display name, child, city,
  appearance, password, sign-out/delete. NO notification section.
- The Android Supabase implementation lives in
  `android/data/.../SupabaseConsumerApi.kt` (RPC/select calls follow the
  supabase-kt `postgrest` API). Use an existing RPC/select call there as the
  syntactic pattern.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile data | `cd android && scripts/with-android-env.sh ./gradlew :data:compileDebugKotlin` | BUILD SUCCESSFUL |
| Compile app | `cd android && scripts/with-android-env.sh ./gradlew :app:compileDebugKotlin` | BUILD SUCCESSFUL |
| Data tests | `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest` | BUILD SUCCESSFUL |

## Scope

**In scope**:
- `android/data/.../com/familyevents/data/Repositories.kt` — add a
  `NotificationPreferences` model + repo methods (`fetchNotificationPreferences`,
  `updateNotificationPreferences`).
- `android/data/.../com/familyevents/data/SupabaseConsumerApi.kt` — implement the
  select + RPC against `user_notification_preferences` / `upsert_notification_preferences`.
- `android/data/.../com/familyevents/data/InMemoryRepositories.kt` and
  `RoomBackedRepositories.kt` — implement the new methods (in-memory returns
  defaults; Room-backed calls the api with fallback to defaults).
- `android/app/.../com/familyevents/app/ProfileDialog.kt` — add a notification
  preferences section (six Material 3 `Switch`es) wired to the repo.
- A unit test under `android/data/src/test/...` for the model defaults/mapping.

**Out of scope** (do NOT touch):
- The backend / Supabase schema — it already exists; do NOT attempt migrations.
- The existing `updateNotificationPreference(enabled: Boolean)` method — leave it
  (it may be used elsewhere); ADD the granular methods alongside. Removing it is a
  separate cleanup.
- iOS code — it is the reference only.

## Git workflow

- Branch: `advisor/011-android-notification-prefs`
- Commit style: `feat(android): granular notification preferences (parity with iOS)`
- Do NOT push unless instructed.

## Steps

### Step 1 (SPIKE — do first): confirm the backend contract from a Supabase-kt call site

Read `android/data/.../SupabaseConsumerApi.kt` and find one existing
`.from("...").select(...)` call and one `.rpc("...", ...)` call to learn the exact
supabase-kt syntax this repo uses (serialization, response decoding). Cross-check
the column/param names against the iOS repo (Current state). Write down (in the PR
description or a comment) the confirmed table name, column names, RPC name, and
param names BEFORE writing the implementation.

**Verify**: you can point to an existing `.rpc(...)` and `.select(...)` in
`SupabaseConsumerApi.kt` and have confirmed the 6 column + 6 `p_`-prefixed param
names match iOS.

**STOP** if the table `user_notification_preferences` or RPC
`upsert_notification_preferences` is not referenced anywhere reachable and you
cannot confirm it exists — report rather than guessing names.

### Step 2: Add the model + repo contract in `:data`

In `Repositories.kt`, add:

```kotlin
data class NotificationPreferences(
    val reminderEmail: Boolean = true,
    val reminderPush: Boolean = true,
    val changeEmail: Boolean = true,
    val changePush: Boolean = true,
    val digestEmail: Boolean = true,
    val digestPush: Boolean = false,
)
```

Add to `ProfileRepository`:

```kotlin
suspend fun fetchNotificationPreferences(userId: UserId): NotificationPreferences
suspend fun updateNotificationPreferences(userId: UserId, prefs: NotificationPreferences): NotificationPreferences
```

**Verify**: `:data` compiles (it won't yet — implementations come next; this step
just defines the contract, compile after Step 3).

### Step 3: Implement in all three repositories

- `InMemoryProfileRepository`: `fetch...` returns `NotificationPreferences()`
  (defaults); `update...` echoes the passed prefs.
- `RoomBacked`/`Supabase` path: `fetch...` calls the api select (fallback to
  defaults on null/error, using `repoOrNull` from plan 006 if present);
  `update...` calls the RPC and returns the persisted row (fallback to the passed
  prefs on failure). Mirror the iOS column/param mapping exactly.
- `SupabaseConsumerApi.kt`: add the `select` + `rpc` calls.

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:compileDebugKotlin && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest` → BUILD SUCCESSFUL.

### Step 4: Add the UI to `ProfileDialog.kt`

Insert a "Notifications" section (after the Appearance block, before the
`HorizontalDivider()`/Password block). Load current prefs in the existing
`LaunchedEffect(userId)` and hold them in `remember(userId) { mutableStateOf(...) }`.
Render six `Switch`es (grouped: Reminders / Changes / Weekly digest, each Email +
Push). On toggle, optimistically update state and `scope.launch { profileRepository.updateNotificationPreferences(userId, next) }`
with failure handling via the existing `errorMessage`/`toUserMessage()` pattern.
Match the existing Compose style in the file (Material 3, `Modifier.fillMaxWidth()`,
`MaterialTheme.typography`).

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.

### Step 5: Build + test

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest :app:compileDebugKotlin` → BUILD SUCCESSFUL.

## Test plan

- Unit test in `:data` for `NotificationPreferences` defaults and the
  in-memory repo round-trip (`update` then `fetch`-style echo). Model after an
  existing `:data` test.
- The Supabase select/rpc path is integration-level and not unit-tested here
  (no test backend); the spike (Step 1) is the correctness gate for names.
- Verification: `./gradlew :data:testDebugUnitTest` → all pass.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `cd android && scripts/with-android-env.sh ./gradlew :data:testDebugUnitTest :app:compileDebugKotlin` → BUILD SUCCESSFUL
- [ ] `ProfileRepository` has `fetchNotificationPreferences` + `updateNotificationPreferences`, implemented in all repo classes
- [ ] `ProfileDialog.kt` renders six notification `Switch`es wired to the repo
- [ ] Column/param names match iOS (`reminder_email`…`digest_push`; `p_`-prefixed RPC params)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- Step 1 cannot confirm the table/RPC/column names — do NOT guess; a wrong name
  silently drops user settings.
- The supabase-kt select/rpc syntax in this repo differs from what you can
  pattern-match — report and ask.
- Adding the UI would require restructuring `ProfileDialog` beyond inserting a
  section (e.g. it overflows the dialog) — report; a full settings screen may be
  the better home (a follow-up decision).

## Maintenance notes

- Keep the Android model in lockstep with iOS `NotificationPreferences`; they
  share a backend contract.
- Follow-up deferred: the legacy single-boolean `updateNotificationPreference`
  can be removed once nothing calls it.
- A reviewer should verify the optimistic-update rollback on RPC failure and that
  defaults match iOS (digestPush defaults false).
