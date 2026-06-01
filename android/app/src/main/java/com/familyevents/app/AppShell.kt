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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyevents.data.EventQuery
import kotlinx.coroutines.launch
import com.familyevents.auth.AuthScreen
import com.familyevents.calendar.CalendarScreen
import com.familyevents.core.CityId
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
import com.familyevents.map.MapScreen
import com.familyevents.plan.PlanScreen
import com.familyevents.platform.LocationProvider
import com.familyevents.platform.PlatformActions
import com.familyevents.saved.SavedScreen

private enum class AppTab(val title: String) {
    Plan("Plan"),
    Explore("Explore"),
    Map("Map"),
    Calendar("Calendar"),
    Saved("Saved");

    @Composable
    fun Icon(selected: Boolean) {
        val imageVector = when (this) {
            Plan -> if (selected) Icons.Filled.DateRange else Icons.Outlined.DateRange
            Explore -> if (selected) Icons.Filled.Explore else Icons.Outlined.Explore
            Map -> if (selected) Icons.Filled.Map else Icons.Outlined.Map
            Calendar -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
            Saved -> if (selected) Icons.Filled.Favorite else Icons.Outlined.Favorite
        }
        Icon(imageVector, contentDescription = null)
    }
}

@Composable
fun FamilyEventsApp(
    config: EnvConfig,
    repositories: RepositoryGraph,
    platformActions: PlatformActions,
    locationProvider: LocationProvider,
    initialUrl: String?,
    themePreference: AppThemePreference,
    onThemePreferenceChange: (AppThemePreference) -> Unit,
) {
    val session by repositories.authRepository.sessionState.collectAsStateWithLifecycle(initialValue = SessionState.Restoring)
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
                    googleWebClientId = config.googleWebClientId,
                )
            }
        }
        is SessionState.SignedIn -> {
            val userId = state.userId
            val profile by repositories.profileRepository.observeProfile(userId).collectAsStateWithLifecycle(initialValue = null)
            val activeCityId = profile?.currentCityId ?: CityId("chicago")
            val cities by repositories.cityRepository.observeCities().collectAsStateWithLifecycle(initialValue = emptyList())
            val scope = rememberCoroutineScope()
            var cityName by remember { mutableStateOf<String?>(null) }
            var showCityPicker by remember { mutableStateOf(false) }

            // Lifecycle-aware foreground refresh
            var foregroundKey by remember { mutableIntStateOf(0) }
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) foregroundKey++
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            LaunchedEffect(foregroundKey) {
                if (foregroundKey <= 1) return@LaunchedEffect
                when (selectedTab) {
                    AppTab.Plan -> {
                        val coord = locationProvider.lastKnownLocation()
                        runCatching { repositories.eventRepository.refreshPlan(userId, activeCityId, profile?.kidAge, coord?.latitude, coord?.longitude) }
                    }
                    AppTab.Explore, AppTab.Map, AppTab.Calendar ->
                        runCatching { repositories.eventRepository.refreshEventList(EventQuery(cityId = activeCityId)) }
                    AppTab.Saved ->
                        runCatching { repositories.eventRepository.refreshEventList(EventQuery(cityId = null)) }
                }
            }

            LaunchedEffect(userId) {
                runCatching { repositories.cityRepository.refreshCities() }
                runCatching { repositories.profileRepository.currentContext(userId) }
            }
            LaunchedEffect(activeCityId) {
                cityName = runCatching { repositories.cityRepository.cityName(activeCityId) }.getOrNull()
            }
            AppScaffold(
                tabs = AppTab.entries,
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
                        cityName = cityName,
                        kidAge = profile?.kidAge,
                        eventRepository = repositories.eventRepository,
                        favoriteRepository = repositories.favoriteRepository,
                        weatherRepository = repositories.weatherRepository,
                        locationProvider = locationProvider,
                        onOpenEvent = { detailEventId = it },
                        onSetCity = { showCityPicker = true },
                    )
                    AppTab.Explore -> ExploreScreen(
                        cityId = activeCityId,
                        eventRepository = repositories.eventRepository,
                        onOpenEvent = { detailEventId = it },
                    )
                    AppTab.Map -> MapScreen(
                        cityId = activeCityId,
                        cityName = cityName,
                        mapStyleUrl = config.mapStyleUrl,
                        eventRepository = repositories.eventRepository,
                        onOpenEvent = { detailEventId = it },
                        onSetCity = { showCityPicker = true },
                    )
                    AppTab.Calendar -> CalendarScreen(
                        cityId = activeCityId,
                        cityName = cityName,
                        eventRepository = repositories.eventRepository,
                        onOpenEvent = { detailEventId = it },
                        onSetCity = { showCityPicker = true },
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
                        ratingRepository = repositories.ratingRepository,
                        commentRepository = repositories.commentRepository,
                        onBack = { detailEventId = null },
                        onShare = platformActions::share,
                        onDirections = platformActions::directions,
                        onAddToCalendar = platformActions::addToCalendar,
                    )
                }
            }

            if (showCityPicker) {
                CityPickerBottomSheet(
                    cities = cities,
                    selectedCityId = activeCityId,
                    onSelectCity = { newCityId ->
                        showCityPicker = false
                        scope.launch {
                            runCatching {
                                repositories.profileRepository.updateContext(userId, newCityId, profile?.kidAge)
                            }
                        }
                    },
                    onDismiss = { showCityPicker = false },
                )
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
    "map" -> AppTab.Map
    "calendar" -> AppTab.Calendar
    "saved" -> AppTab.Saved
    else -> AppTab.Plan
}
