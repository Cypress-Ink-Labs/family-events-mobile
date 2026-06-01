package com.familyevents.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.GeoCoordinate
import com.familyevents.data.EventDto
import com.familyevents.data.EventQuery
import com.familyevents.data.EventRepository
import androidx.compose.material3.FilterChip
import com.familyevents.designsystem.EventCard
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.generated.Tokens
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun MapScreen(
    cityId: CityId?,
    cityName: String? = null,
    mapStyleUrl: String,
    eventRepository: EventRepository,
    onOpenEvent: (EventId) -> Unit,
    onSetCity: (() -> Unit)? = null,
) {
    val events by eventRepository.observeEventList(EventQuery(cityId = cityId)).collectAsStateWithLifecycle(initialValue = emptyList())
    var selectedEvent by remember { mutableStateOf<EventDto?>(null) }

    LaunchedEffect(cityId) {
        runCatching { eventRepository.refreshEventList(EventQuery(cityId = cityId)) }
    }

    val points = remember(events) {
        events.mapNotNull { event ->
            event.coordinate?.let { coord -> event to coord }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            if (onSetCity != null) {
                Box(Modifier.padding(start = Tokens.Space.S4, top = Tokens.Space.S2, end = Tokens.Space.S4)) {
                    FilterChip(
                        selected = cityName != null,
                        onClick = onSetCity,
                        label = { Text(cityName ?: "Select city") },
                    )
                }
            }
            if (points.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(Tokens.Space.S4),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No mappable events yet.", style = FamilyTypography.Body)
                }
            } else {
                MapContent(
                    styleUrl = mapStyleUrl,
                    points = points,
                    onMarkerTap = { event -> selectedEvent = event },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        AnimatedVisibility(
            visible = selectedEvent != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Tokens.Space.S4),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            selectedEvent?.let { event ->
                EventPopupCard(
                    event = event,
                    onOpenEvent = {
                        onOpenEvent(event.id)
                        selectedEvent = null
                    },
                    onDismiss = { selectedEvent = null },
                )
            }
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun MapContent(
    styleUrl: String,
    points: List<Pair<EventDto, GeoCoordinate>>,
    onMarkerTap: (EventDto) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { it.onCreate(null) }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        onDispose {
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        update = { view ->
            view.getMapAsync { map ->
                map.setStyle(Style.Builder().fromUri(styleUrl))
                map.clear()
                val markerEventMap = mutableMapOf<Long, EventDto>()
                points.forEach { (event, coord) ->
                    val marker = map.addMarker(
                        org.maplibre.android.annotations.MarkerOptions()
                            .position(LatLng(coord.latitude, coord.longitude))
                            .title(event.title),
                    )
                    markerEventMap[marker.id] = event
                }
                map.setOnMarkerClickListener { marker ->
                    markerEventMap[marker.id]?.let(onMarkerTap)
                    true
                }
                points.firstOrNull()?.second?.let { coord ->
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(coord.latitude, coord.longitude), 11.0),
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize(),
    )
}

@Composable
private fun EventPopupCard(
    event: EventDto,
    onOpenEvent: () -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenEvent,
    ) {
        EventCard(
            title = event.title,
            subtitle = event.venueName ?: "Family event",
            badge = event.tags.firstOrNull()?.label,
            imageUrl = event.imageUrl,
            onClick = onOpenEvent,
        )
    }
}
