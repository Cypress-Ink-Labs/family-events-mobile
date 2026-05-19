package com.familyevents.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.familyevents.core.CityId
import com.familyevents.auth.AuthScreen
import com.familyevents.core.DeepLinkPolicy
import com.familyevents.core.DeepLinkTarget
import com.familyevents.core.EnvConfig
import com.familyevents.core.EventId
import com.familyevents.data.RepositoryGraph
import com.familyevents.data.SessionState
import com.familyevents.designsystem.AppThemePreference
import com.familyevents.eventdetail.EventDetailScreen
import com.familyevents.eventdetail.PublicSharePreviewScreen
import com.familyevents.explore.ExploreScreen
import com.familyevents.plan.PlanScreen
import com.familyevents.platform.PlatformActions
import com.familyevents.saved.SavedScreen

private enum class AppTab(val title: String) {
    Plan("Plan"),
    Explore("Explore"),
    Saved("Saved"),
    Admin("Admin");

    @Composable
    fun Icon(selected: Boolean) {
        val imageVector = when (this) {
            Plan -> if (selected) Icons.Filled.DateRange else Icons.Outlined.DateRange
            Explore -> if (selected) Icons.Filled.Explore else Icons.Outlined.Explore
            Saved -> if (selected) Icons.Filled.Favorite else Icons.Outlined.Favorite
            Admin -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
        }
        Icon(imageVector, contentDescription = null)
    }
}

@Composable
fun FamilyEventsApp(
    config: EnvConfig,
    repositories: RepositoryGraph,
    platformActions: PlatformActions,
    initialUrl: String?,
    themePreference: AppThemePreference,
    onThemePreferenceChange: (AppThemePreference) -> Unit,
) {
    val session by repositories.authRepository.sessionState.collectAsState(initial = SessionState.Restoring)
    val signedInUser = (session as? SessionState.SignedIn)?.userId
    var selectedTab by remember { mutableStateOf(AppTab.Plan) }
    var detailEventId by remember { mutableStateOf<EventId?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    var pendingShareEventId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        repositories.authRepository.restoreSession()
    }

    LaunchedEffect(initialUrl) {
        val target = initialUrl?.let(DeepLinkPolicy::parse)
        if (target is DeepLinkTarget.Share) {
            pendingShareEventId = target.id.rawValue
        }
        routeDeepLink(initialUrl, onTab = { selectedTab = it }, onEvent = { detailEventId = it })
    }

    when (val state = session) {
        SessionState.Restoring -> Box(Modifier.fillMaxSize())
        SessionState.SignedOut -> {
            val sharePreview = pendingShareEventId
            if (sharePreview != null) {
                PublicSharePreviewScreen(
                    eventId = EventId(sharePreview),
                    eventRepository = repositories.eventRepository,
                    onSignIn = { pendingShareEventId = null },
                )
            } else {
                AuthScreen(
                    authRepository = repositories.authRepository,
                    googleSignInEnabled = config.googleSignInEnabled,
                )
            }
        }
        is SessionState.SignedIn -> {
            val userId = state.userId
            val profile by repositories.profileRepository.observeProfile(userId).collectAsState(initial = null)
            val activeCityId = profile?.currentCityId ?: CityId("chicago")
            val showAdminTab = profile?.role == "admin"
            LaunchedEffect(userId) {
                repositories.cityRepository.refreshCities()
                repositories.profileRepository.currentContext(userId)
            }
            val visibleTabs = if (showAdminTab) AppTab.entries else AppTab.entries.filter { it != AppTab.Admin }
            AppScaffold(
                tabs = visibleTabs,
                selectedTab = selectedTab,
                onSelectTab = {
                    selectedTab = it
                    detailEventId = null
                },
            ) { contentModifier ->
                when (selectedTab) {
                    AppTab.Plan -> PlanScreen(
                        userId = userId,
                        cityId = activeCityId,
                        kidAge = profile?.kidAge,
                        eventRepository = repositories.eventRepository,
                        favoriteRepository = repositories.favoriteRepository,
                        weatherRepository = repositories.weatherRepository,
                        onOpenEvent = { detailEventId = it },
                        onSetCity = { showProfile = true },
                    )
                    AppTab.Explore -> ExploreScreen(
                        cityId = activeCityId,
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
                    AppTab.Admin -> AdminScreen(
                        adminRepository = repositories.adminRepository,
                        modifier = contentModifier,
                    )
                }
                detailEventId?.let { eventId ->
                    EventDetailScreen(
                        eventId = eventId,
                        userId = userId,
                        eventRepository = repositories.eventRepository,
                        favoriteRepository = repositories.favoriteRepository,
                        ratingRepository = repositories.ratingRepository,
                        commentRepository = repositories.commentRepository,
                        onBack = { detailEventId = null },
                        onShare = platformActions::share,
                        onDirections = platformActions::directions,
                        onAddToCalendar = platformActions::addToCalendar,
                    )
                }
            }
        }
    }

    if (showProfile && signedInUser != null) ProfileDialog(
        userId = signedInUser,
        authRepository = repositories.authRepository,
        profileRepository = repositories.profileRepository,
        cityRepository = repositories.cityRepository,
        themePreference = themePreference,
        onThemePreferenceChange = onThemePreferenceChange,
        onDismissRequest = { showProfile = false },
    )
}

@Composable
private fun AppScaffold(
    tabs: List<AppTab>,
    selectedTab: AppTab,
    onSelectTab: (AppTab) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val wide = LocalConfiguration.current.screenWidthDp >= 720
    if (wide) {
        Row(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            NavigationRail {
                tabs.forEach { tab ->
                    NavigationRailItem(
                        selected = selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        icon = { tab.Icon(selected = selectedTab == tab) },
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
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            icon = { tab.Icon(selected = selectedTab == tab) },
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
        is DeepLinkTarget.Tab -> onTab(target.tab.toAppTab())
        null -> Unit
    }
}

private fun String.toAppTab(): AppTab = when (lowercase()) {
    "explore" -> AppTab.Explore
    "saved" -> AppTab.Saved
    else -> AppTab.Plan
}
