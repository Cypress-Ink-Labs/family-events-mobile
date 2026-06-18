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
