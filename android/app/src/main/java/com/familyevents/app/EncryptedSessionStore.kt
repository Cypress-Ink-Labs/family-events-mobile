package com.familyevents.app

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.familyevents.core.UserId
import com.familyevents.data.SessionStore

class EncryptedSessionStore(context: Context) : SessionStore {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "family_events_session",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun readUserId(): UserId? =
        prefs.getString(UserIdKey, null)?.takeIf { it.isNotBlank() }?.let(::UserId)

    override suspend fun writeUserId(userId: UserId?) {
        prefs.edit().apply {
            if (userId == null) remove(UserIdKey) else putString(UserIdKey, userId.rawValue)
        }.apply()
    }

    private companion object {
        const val UserIdKey = "user_id"
    }
}
