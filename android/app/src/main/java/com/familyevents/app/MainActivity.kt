package com.familyevents.app

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.familyevents.core.EnvConfig
import com.familyevents.data.RepositoryGraph
import com.familyevents.designsystem.AppThemePreference
import com.familyevents.designsystem.FamilyEventsTheme
import com.familyevents.platform.PlatformActions
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var config: EnvConfig
    @Inject lateinit var repositories: RepositoryGraph
    @Inject lateinit var platformActions: PlatformActions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        publishShortcuts()
        setContent {
            val preferences = remember { getSharedPreferences("family-events", MODE_PRIVATE) }
            var themeRawValue by remember { mutableStateOf(preferences.getString("family-events-theme", "system") ?: "system") }
            val themePreference = AppThemePreference.fromRawValue(themeRawValue)
            FamilyEventsTheme(darkTheme = themePreference.useDarkTheme(isSystemInDarkTheme())) {
                FamilyEventsApp(
                    config = config,
                    repositories = repositories,
                    platformActions = platformActions,
                    initialUrl = intent?.data?.toString(),
                    themePreference = themePreference,
                    onThemePreferenceChange = { preference ->
                        themeRawValue = preference.rawValue
                        preferences.edit().putString("family-events-theme", preference.rawValue).apply()
                    },
                )
            }
        }
    }

    private fun publishShortcuts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
        val manager = getSystemService(ShortcutManager::class.java) ?: return
        val icon = Icon.createWithResource(this, applicationInfo.icon)
        manager.dynamicShortcuts = listOf(
            shortcut("plan", "Plan", "Open Saturday Plan", "familyevents://tab/plan", icon),
            shortcut("explore", "Explore", "Search family events", "familyevents://tab/explore", icon),
            shortcut("saved", "Saved", "Open saved events", "familyevents://tab/saved", icon),
            shortcut("admin", "Admin", "Open the admin dashboard", "familyevents://admin/dashboard", icon),
        )
    }

    private fun shortcut(id: String, label: String, longLabel: String, uri: String, icon: Icon): ShortcutInfo =
        ShortcutInfo.Builder(this, id)
            .setShortLabel(label)
            .setLongLabel(longLabel)
            .setIcon(icon)
            .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse(uri), this, MainActivity::class.java))
            .build()
}
