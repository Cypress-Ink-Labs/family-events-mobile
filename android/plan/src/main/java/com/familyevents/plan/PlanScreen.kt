package com.familyevents.plan

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.UserId
import com.familyevents.data.EventRepository
import com.familyevents.data.FavoriteRepository
import com.familyevents.data.PlanEventRowDto
import com.familyevents.data.WeatherRepository
import com.familyevents.data.WeatherSnapshotDto
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.EventHeroImage
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.TagPill
import com.familyevents.designsystem.generated.Tokens
import com.familyevents.platform.LocationProvider
import kotlinx.coroutines.launch

@Composable
fun PlanScreen(
    userId: UserId,
    cityId: CityId?,
    cityName: String? = null,
    kidAge: Int? = null,
    eventRepository: EventRepository,
    favoriteRepository: FavoriteRepository,
    weatherRepository: WeatherRepository,
    locationProvider: LocationProvider,
    onOpenEvent: (EventId) -> Unit,
    onSetCity: () -> Unit,
) {
    val rows by eventRepository.observePlanEvents(userId, cityId).collectAsStateWithLifecycle(initialValue = emptyList())
    val emptyForecast = remember { mutableStateOf(emptyList<WeatherSnapshotDto>()) }
    val forecast by cityId?.let { weatherRepository.observeForecast(it).collectAsStateWithLifecycle(initialValue = emptyList()) } ?: emptyForecast
    val scope = rememberCoroutineScope()
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val currentUserId by rememberUpdatedState(userId)
    val currentCityId by rememberUpdatedState(cityId)
    val currentKidAge by rememberUpdatedState(kidAge)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val anyGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        scope.launch {
            val coord = if (anyGranted) locationProvider.lastKnownLocation() else null
            runCatching { eventRepository.refreshPlan(currentUserId, currentCityId, currentKidAge, coord?.latitude, coord?.longitude) }
        }
    }

    LaunchedEffect(userId, cityId, kidAge) {
        val coord = locationProvider.lastKnownLocation()
        runCatching { eventRepository.refreshPlan(userId, cityId, kidAge, coord?.latitude, coord?.longitude) }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                val coord = locationProvider.lastKnownLocation()
                if (coord == null && !permissionAsked) {
                    permissionAsked = true
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                } else {
                    runCatching { eventRepository.refreshPlan(userId, cityId, kidAge, coord?.latitude, coord?.longitude) }
                }
                if (cityId != null) {
                    runCatching { weatherRepository.refreshForecast(cityId) }
                }
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4),
            contentPadding = PaddingValues(Tokens.Space.S4),
        ) {
            item {
                Text("Saturday Plan", style = FamilyTypography.TitleLarge)
            }
            item {
                PlanContextBar(cityName = cityName, kidAge = kidAge, onSetCity = onSetCity)
            }
            if (forecast.isNotEmpty()) {
                item {
                    WeatherStrip(snapshot = forecast.first())
                }
            }
            if (cityId == null || rows.isEmpty()) {
                item {
                    EmptyState("Set your city or broaden filters.", "Set city", onSetCity)
                }
            } else {
                item {
                    PlanHeroCard(row = rows.first(), onOpen = { onOpenEvent(it) })
                }
                if (rows.size > 1) {
                    item {
                        Text("Also this week", style = FamilyTypography.TitleMedium)
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
                        ) {
                            items(rows.drop(1), key = { it.event.id.rawValue }) { row ->
                                PlanCarouselCard(row = row, onOpen = { onOpenEvent(it) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanContextBar(cityName: String?, kidAge: Int?, onSetCity: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
        FilterChip(
            selected = cityName != null,
            onClick = onSetCity,
            label = { Text(cityName ?: "Set city") },
        )
        if (kidAge != null) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Age $kidAge") },
            )
        }
    }
}

@Composable
private fun WeatherStrip(snapshot: WeatherSnapshotDto) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Tokens.Radius.Md),
    ) {
        Row(
            modifier = Modifier.padding(Tokens.Space.S3),
            horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Tokens.Space.S1)) {
                Text(snapshot.summary, style = FamilyTypography.Body)
                Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S3)) {
                    snapshot.temperatureHighF?.let { Text("${it}°F", style = FamilyTypography.BodySmall) }
                    snapshot.precipitationChance?.let {
                        Text("${(it * 100).toInt()}% rain", style = FamilyTypography.BodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanHeroCard(row: PlanEventRowDto, onOpen: (EventId) -> Unit) {
    Card(
        onClick = { onOpen(row.event.id) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Tokens.Radius.Md),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column {
            EventHeroImage(title = row.event.title, imageUrl = row.event.imageUrl)
            Column(
                modifier = Modifier.padding(Tokens.Space.S4),
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.S2),
            ) {
                Text(row.event.title, style = FamilyTypography.TitleLarge)
                Text(
                    row.event.venueName ?: row.section,
                    style = FamilyTypography.Body,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                row.event.tags.firstOrNull()?.let { TagPill(it.label) }
            }
        }
    }
}

@Composable
private fun PlanCarouselCard(row: PlanEventRowDto, onOpen: (EventId) -> Unit) {
    OutlinedCard(
        onClick = { onOpen(row.event.id) },
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(Tokens.Radius.Md),
    ) {
        Column(
            modifier = Modifier.padding(Tokens.Space.S3),
            verticalArrangement = Arrangement.spacedBy(Tokens.Space.S1),
        ) {
            Text(
                row.event.title,
                style = FamilyTypography.TitleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                row.event.venueName ?: row.section,
                style = FamilyTypography.BodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            row.event.tags.firstOrNull()?.let { TagPill(it.label) }
        }
    }
}
