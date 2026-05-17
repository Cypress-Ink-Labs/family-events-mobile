package com.familyevents.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.familyevents.core.EnvConfig
import com.familyevents.data.RepositoryGraph
import com.familyevents.designsystem.FamilyEventsTheme

class MainActivity : ComponentActivity() {
    private val repositories = RepositoryGraph()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = EnvConfig(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY,
            mapStyleUrl = BuildConfig.MAP_STYLE_URL,
            googleWebClientId = BuildConfig.ANDROID_GOOGLE_WEB_CLIENT_ID.ifBlank { null },
        )
        setContent {
            FamilyEventsTheme {
                FamilyEventsApp(
                    config = config,
                    repositories = repositories,
                    initialUrl = intent?.data?.toString(),
                )
            }
        }
    }
}
