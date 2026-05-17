package com.familyevents.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.familyevents.auth.AuthScreen
import com.familyevents.core.DeepLinkPolicy
import com.familyevents.core.DeepLinkTarget
import com.familyevents.core.EnvConfig
import com.familyevents.core.EventId
import com.familyevents.data.RepositoryGraph
import com.familyevents.data.SessionState
import com.familyevents.eventdetail.EventDetailScreen
import com.familyevents.explore.ExploreScreen
import com.familyevents.plan.PlanScreen
import com.familyevents.platform.PlatformActions
import com.familyevents.saved.SavedScreen
import kotlinx.coroutines.launch

private enum class AppTab(val title: String) {
    Plan("Plan"),
    Explore("Explore"),
    Saved("Saved"),
}

@Composable
fun FamilyEventsApp(
    config: EnvConfig,
    repositories: RepositoryGraph,
    platformActions: PlatformActions,
    initialUrl: String?,
) {
    val session by repositories.authRepository.sessionState.collectAsState(initial = SessionState.Restoring)
    val signedInUser = (session as? SessionState.SignedIn)?.userId
    var selectedTab by remember { mutableStateOf(AppTab.Plan) }
    var detailEventId by remember { mutableStateOf<EventId?>(null) }
    var showProfile by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repositories.authRepository.restoreSession()
    }

    LaunchedEffect(initialUrl) {
        routeDeepLink(initialUrl, onTab = { selectedTab = it }, onEvent = { detailEventId = it })
    }

    when (val state = session) {
        SessionState.Restoring -> Box(Modifier.fillMaxSize())
        SessionState.SignedOut -> AuthScreen(
            authRepository = repositories.authRepository,
            googleSignInEnabled = config.googleSignInEnabled,
        )
        is SessionState.SignedIn -> {
            AppScaffold(selectedTab = selectedTab, onSelectTab = { selectedTab = it }) { contentModifier ->
                val userId = state.userId
                when (selectedTab) {
                    AppTab.Plan -> PlanScreen(
                        userId = userId,
                        cityId = com.familyevents.core.CityId("chicago"),
                        eventRepository = repositories.eventRepository,
                        favoriteRepository = repositories.favoriteRepository,
                        weatherRepository = repositories.weatherRepository,
                        onOpenEvent = { detailEventId = it },
                        onSetCity = { showProfile = true },
                    )
                    AppTab.Explore -> ExploreScreen(
                        cityId = com.familyevents.core.CityId("chicago"),
                        mapStyleUrl = config.mapStyleUrl,
                        eventRepository = repositories.eventRepository,
                        onOpenEvent = { detailEventId = it },
                    )
                    AppTab.Saved -> SavedScreen(
                        userId = userId,
                        eventRepository = repositories.eventRepository,
                        favoriteRepository = repositories.favoriteRepository,
                        onOpenEvent = { detailEventId = it },
                        onOpenProfile = { showProfile = true },
                    )
                }
                detailEventId?.let { eventId ->
                    EventDetailScreen(
                        eventId = eventId,
                        userId = userId,
                        eventRepository = repositories.eventRepository,
                        favoriteRepository = repositories.favoriteRepository,
                        onShare = platformActions::share,
                        onDirections = platformActions::directions,
                        onAddToCalendar = platformActions::addToCalendar,
                    )
                }
            }
        }
    }

    if (showProfile && signedInUser != null) {
        val profile by repositories.profileRepository.observeProfile(signedInUser).collectAsState(initial = null)
        val scope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { showProfile = false },
            title = { Text("Profile") },
            text = {
                Text(
                    "City: ${profile?.currentCityId?.rawValue ?: "chicago"}\n" +
                        "Kid age: ${profile?.kidAge ?: 7}\n" +
                        "Notifications: ${if (profile?.notificationsEnabled == true) "on" else "off"}",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repositories.profileRepository.updateNotificationPreference(
                            signedInUser,
                            enabled = profile?.notificationsEnabled != true,
                        )
                    }
                }) { Text("Notifications") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showProfile = false
                    scope.launch { repositories.authRepository.signOut() }
                }) { Text("Sign out") }
            },
        )
    }
}

@Composable
private fun AppScaffold(
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 720
    if (wide) {
        Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            NavigationRail {
                AppTab.entries.forEach { tab ->
                    NavigationRailItem(
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        icon = { IconPlaceholder(tab.title) },
                        label = { Text(tab.title) },
                    )
                }
            }
            Box(Modifier.weight(1f)) { content(Modifier.fillMaxSize()) }
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    AppTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            icon = { IconPlaceholder(tab.title) },
                            label = { Text(tab.title) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) { content(Modifier.fillMaxSize()) }
        }
    }
}

@Composable
private fun IconPlaceholder(label: String) {
    Text(label.take(1))
}

private fun routeDeepLink(raw: String?, onTab: (AppTab) -> Unit, onEvent: (EventId) -> Unit) {
    when (val target = raw?.let(DeepLinkPolicy::parse)) {
        is DeepLinkTarget.Event -> {
            onTab(AppTab.Plan)
            onEvent(target.id)
        }
        is DeepLinkTarget.Share -> {
            onTab(AppTab.Plan)
            onEvent(target.id)
        }
        is DeepLinkTarget.City -> onTab(AppTab.Explore)
        is DeepLinkTarget.ResetPassword -> onTab(AppTab.Saved)
        null -> Unit
    }
}
