package com.familyevents.app

import android.content.Context
import com.familyevents.core.UserId
import com.familyevents.data.PersistedSession
import com.familyevents.data.SessionStore

class EncryptedSessionStore(context: Context) : SessionStore {
    @Suppress("DEPRECATION")
    private val prefs = androidx.security.crypto.EncryptedSharedPreferences.create(
        context,
        "family_events_session",
        androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build(),
        androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun readSession(): PersistedSession? =
        prefs.getString(UserIdKey, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { userId ->
                PersistedSession(
                    userId = UserId(userId),
                    accessToken = prefs.getString(AccessTokenKey, null)?.takeIf(String::isNotBlank),
                    refreshToken = prefs.getString(RefreshTokenKey, null)?.takeIf(String::isNotBlank),
                )
            }

    override suspend fun writeSession(session: PersistedSession?) {
        prefs.edit().apply {
            if (session == null) {
                remove(UserIdKey)
                remove(AccessTokenKey)
                remove(RefreshTokenKey)
            } else {
                putString(UserIdKey, session.userId.rawValue)
                if (session.accessToken == null) remove(AccessTokenKey) else putString(AccessTokenKey, session.accessToken)
                if (session.refreshToken == null) remove(RefreshTokenKey) else putString(RefreshTokenKey, session.refreshToken)
            }
        }.apply()
    }

    private companion object {
        const val UserIdKey = "user_id"
        const val AccessTokenKey = "access_token"
        const val RefreshTokenKey = "refresh_token"
    }
}
