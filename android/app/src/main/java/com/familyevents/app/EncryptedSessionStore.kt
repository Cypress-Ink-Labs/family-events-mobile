package com.familyevents.app

import android.content.Context
import android.content.SharedPreferences
import com.familyevents.data.PersistedSession
import com.familyevents.data.SessionStore

class EncryptedSessionStore(context: Context) : SessionStore {
    private val appContext = context.applicationContext

    @Suppress("DEPRECATION")
    private val prefs: SharedPreferences by lazy {
        androidx.security.crypto.EncryptedSharedPreferences.create(
            appContext,
            "family_events_session",
            androidx.security.crypto.MasterKey.Builder(appContext)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build(),
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

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

    private fun prefsOrNull(): SharedPreferences? = runCatching { prefs }.getOrNull()
}
