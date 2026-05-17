package com.familyevents.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.familyevents.core.EnvConfig
import com.familyevents.data.RepositoryGraph
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
            FamilyEventsTheme {
                FamilyEventsApp(
                    config = config,
                    repositories = repositories,
                    platformActions = platformActions,
                    initialUrl = intent?.data?.toString(),
                )
            }
        }
    }
}
