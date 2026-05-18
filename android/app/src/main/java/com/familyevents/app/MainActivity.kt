package com.familyevents.app

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
}
