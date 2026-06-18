# Plan 007: Make Android session persistence testable and cover it with unit tests

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat 744e1c6..HEAD -- android/app/src/main/java/com/familyevents/app/EncryptedSessionStore.kt`
> If the file changed since this plan was written, compare the "Current state"
> excerpt against the live code before proceeding; on a mismatch, STOP.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: LOW
- **Depends on**: 001 (so the new tests run in CI)
- **Category**: tests
- **Planned at**: commit `744e1c6`, 2026-06-17

## Why this matters

Session persistence is an auth-critical path: it decides whether a user stays
signed in and whether tokens are correctly cleared on logout. On Android it is
implemented by `EncryptedSessionStore` and has **zero test coverage** (iOS has 15
auth tests by comparison). The security-relevant logic — "a blank stored token
must be treated as no token", "logout must remove all three keys", "a missing
userId means no session" — is currently entangled with `EncryptedSharedPreferences`,
which needs an Android `Context` and the Android Keystore and so cannot be unit-
tested on the JVM (and is flaky under Robolectric). This plan extracts that pure
mapping logic into a `SessionCodec` seam and unit-tests it with no new
dependencies, leaving the encryption setup as thin, untested framework glue.

## Current state

`android/app/src/main/java/com/familyevents/app/EncryptedSessionStore.kt`:

```kotlin
class EncryptedSessionStore(context: Context) : SessionStore {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy { /* EncryptedSharedPreferences.create(...) */ }

    override suspend fun readSession(): PersistedSession? {
        val preferences = prefsOrNull() ?: return null
        return preferences.getString(UserIdKey, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { userId ->
                PersistedSession(
                    userId = UserId(userId),
                    accessToken = preferences.getString(AccessTokenKey, null)?.takeIf(String::isNotBlank),
                    refreshToken = preferences.getString(RefreshTokenKey, null)?.takeIf(String::isNotBlank),
                )
            }
    }

    override suspend fun writeSession(session: PersistedSession?) {
        prefsOrNull()?.edit()?.apply {
            if (session == null) {
                remove(UserIdKey); remove(AccessTokenKey); remove(RefreshTokenKey)
            } else {
                putString(UserIdKey, session.userId.rawValue)
                if (session.accessToken == null) remove(AccessTokenKey) else putString(AccessTokenKey, session.accessToken)
                if (session.refreshToken == null) remove(RefreshTokenKey) else putString(RefreshTokenKey, session.refreshToken)
            }
        }?.apply()
    }

    private fun prefsOrNull(): SharedPreferences? = runCatching { prefs }.getOrNull()

    private companion object {
        const val UserIdKey = "user_id"
        const val AccessTokenKey = "access_token"
        const val RefreshTokenKey = "refresh_token"
    }
}
```

- `SessionStore` (interface) and `PersistedSession(userId: UserId, accessToken: String?, refreshToken: String?)`
  live in `android/data/.../com/familyevents/data/` (package `com.familyevents.data`).
- `:app` test deps: only `testImplementation(libs.junit)` (JUnit 4). No Robolectric.
  The codec test must use plain JUnit — no Android types in the test.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Compile app | `cd android && scripts/with-android-env.sh ./gradlew :app:compileDebugKotlin` | BUILD SUCCESSFUL |
| Run app unit tests | `cd android && scripts/with-android-env.sh ./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL |

## Scope

**In scope**:
- `android/app/src/main/java/com/familyevents/app/SessionCodec.kt` (create)
- `android/app/src/main/java/com/familyevents/app/EncryptedSessionStore.kt` (refactor to delegate to the codec)
- `android/app/src/test/java/com/familyevents/app/SessionCodecTest.kt` (create)

**Out of scope** (do NOT touch):
- The `EncryptedSharedPreferences.create(...)` setup — leave the encryption config
  exactly as is; it stays untested framework glue.
- The `SessionStore` interface or `PersistedSession` in `:data`.
- Adding any new dependency (no Robolectric, no AndroidX test). If you believe a
  dependency is required, that is a STOP condition.

## Git workflow

- Branch: `advisor/007-session-codec-tests`
- Commit style: `test(android): extract SessionCodec seam and cover session mapping`
- Do NOT push unless instructed.

## Steps

### Step 1: Create the pure `SessionCodec`

Create `android/app/src/main/java/com/familyevents/app/SessionCodec.kt`. It models
the read/write mapping over plain string key-values, with no Android dependency:

```kotlin
package com.familyevents.app

import com.familyevents.core.UserId
import com.familyevents.data.PersistedSession

/** Pure mapping between PersistedSession and the three stored string values.
 *  Kept Android-free so it is unit-testable on the JVM. */
internal object SessionCodec {
    const val UserIdKey = "user_id"
    const val AccessTokenKey = "access_token"
    const val RefreshTokenKey = "refresh_token"

    /** Decode from a key→value getter. Blank/absent userId ⇒ null session;
     *  blank tokens ⇒ null token. */
    fun decode(get: (String) -> String?): PersistedSession? =
        get(UserIdKey)?.takeIf { it.isNotBlank() }?.let { userId ->
            PersistedSession(
                userId = UserId(userId),
                accessToken = get(AccessTokenKey)?.takeIf(String::isNotBlank),
                refreshToken = get(RefreshTokenKey)?.takeIf(String::isNotBlank),
            )
        }

    /** Map a session (or null for logout) to the set of writes to apply.
     *  A null value means "remove this key". */
    fun encode(session: PersistedSession?): Map<String, String?> =
        if (session == null) {
            mapOf(UserIdKey to null, AccessTokenKey to null, RefreshTokenKey to null)
        } else {
            mapOf(
                UserIdKey to session.userId.rawValue,
                AccessTokenKey to session.accessToken,
                RefreshTokenKey to session.refreshToken,
            )
        }
}
```

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.

### Step 2: Make `EncryptedSessionStore` delegate to the codec

Refactor `readSession`/`writeSession` to call `SessionCodec`, preserving identical
runtime behavior:

```kotlin
    override suspend fun readSession(): PersistedSession? {
        val preferences = prefsOrNull() ?: return null
        return SessionCodec.decode { key -> preferences.getString(key, null) }
    }

    override suspend fun writeSession(session: PersistedSession?) {
        val preferences = prefsOrNull() ?: return
        preferences.edit().apply {
            SessionCodec.encode(session).forEach { (key, value) ->
                if (value == null) remove(key) else putString(key, value)
            }
        }.apply()
    }
```

Remove the now-duplicated `UserIdKey`/`AccessTokenKey`/`RefreshTokenKey` constants
from `EncryptedSessionStore`'s companion object (they live in `SessionCodec` now);
update any internal references. Keep the `prefs`/`prefsOrNull` encryption setup
unchanged.

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL.

### Step 3: Write the unit test

Create `android/app/src/test/java/com/familyevents/app/SessionCodecTest.kt`
(JUnit 4, no Android imports):

```kotlin
package com.familyevents.app

import com.familyevents.core.UserId
import com.familyevents.data.PersistedSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCodecTest {
    private fun store(vararg pairs: Pair<String, String?>): (String) -> String? {
        val map = pairs.toMap()
        return { key -> map[key] }
    }

    @Test fun decode_returnsNull_whenUserIdMissing() {
        assertNull(SessionCodec.decode(store()))
    }

    @Test fun decode_returnsNull_whenUserIdBlank() {
        assertNull(SessionCodec.decode(store(SessionCodec.UserIdKey to "  ")))
    }

    @Test fun decode_mapsTokens_whenPresent() {
        val s = SessionCodec.decode(store(
            SessionCodec.UserIdKey to "u1",
            SessionCodec.AccessTokenKey to "a",
            SessionCodec.RefreshTokenKey to "r",
        ))
        assertEquals(UserId("u1"), s?.userId)
        assertEquals("a", s?.accessToken)
        assertEquals("r", s?.refreshToken)
    }

    @Test fun decode_treatsBlankTokensAsNull() {
        val s = SessionCodec.decode(store(
            SessionCodec.UserIdKey to "u1",
            SessionCodec.AccessTokenKey to "",
            SessionCodec.RefreshTokenKey to "   ",
        ))
        assertNull(s?.accessToken)
        assertNull(s?.refreshToken)
    }

    @Test fun encode_null_removesAllKeys() {
        val w = SessionCodec.encode(null)
        assertNull(w[SessionCodec.UserIdKey])
        assertNull(w[SessionCodec.AccessTokenKey])
        assertNull(w[SessionCodec.RefreshTokenKey])
    }

    @Test fun encode_session_writesUserIdAndPassesThroughNullTokens() {
        val w = SessionCodec.encode(PersistedSession(UserId("u1"), accessToken = null, refreshToken = "r"))
        assertEquals("u1", w[SessionCodec.UserIdKey])
        assertNull(w[SessionCodec.AccessTokenKey])
        assertEquals("r", w[SessionCodec.RefreshTokenKey])
    }
}
```

**Verify**: `cd android && scripts/with-android-env.sh ./gradlew :app:testDebugUnitTest`
→ BUILD SUCCESSFUL; 6 new tests pass.

## Test plan

- New file `SessionCodecTest.kt` with the 6 cases above (missing userId, blank
  userId, full mapping, blank tokens→null, logout clears all, null-token write).
- No existing Android unit test to model after in `:app` (it has none — this is
  the first); the structure above is self-contained JUnit 4.
- Verification: `./gradlew :app:testDebugUnitTest` → all pass including the 6 new.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `cd android && scripts/with-android-env.sh ./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL with 6 new passing tests
- [ ] `EncryptedSessionStore` read/write delegate to `SessionCodec` (no duplicated key constants remain in the store)
- [ ] The `EncryptedSharedPreferences.create(...)` block is unchanged (diff shows no edit inside `prefs`)
- [ ] No new dependency added (`git diff android/gradle/libs.versions.toml android/app/build.gradle.kts` is empty)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row updated

## STOP conditions

Stop and report back (do not improvise) if:

- `PersistedSession`'s constructor signature differs from
  `(userId: UserId, accessToken: String?, refreshToken: String?)`.
- The refactor would change observable behavior (e.g. you cannot reproduce the
  exact null/blank semantics through the codec).
- You conclude a test dependency (Robolectric/AndroidX test) is required —
  report; do not add one.

## Maintenance notes

- Deferred follow-up: the `auth` module's `AuthScreen` (Compose UI) still has no
  tests. Covering it needs a Compose UI-test harness (`androidx.compose.ui:ui-test-junit4`
  + Robolectric or an instrumented test target) — a separate decision because it
  adds test infrastructure. Note it in `plans/README.md` "considered" if not taken.
- A reviewer should confirm the encryption setup was untouched and that logout
  still removes all three keys (the security-relevant case).
