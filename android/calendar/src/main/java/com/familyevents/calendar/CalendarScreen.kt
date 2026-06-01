package com.familyevents.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyevents.core.CityId
import com.familyevents.core.EventId
import com.familyevents.core.decodeHtmlEntities
import com.familyevents.data.EventDto
import com.familyevents.data.EventQuery
import com.familyevents.data.EventRepository
import androidx.compose.material3.FilterChip
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.EventCard
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.generated.Tokens
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private val DAY_HEADERS = listOf("S", "M", "T", "W", "T", "F", "S")

@Composable
fun CalendarScreen(
    cityId: CityId?,
    cityName: String? = null,
    eventRepository: EventRepository,
    onOpenEvent: (EventId) -> Unit,
    onSetCity: (() -> Unit)? = null,
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val locale: Locale = LocalConfiguration.current.locales[0]
    val today = remember { LocalDate.now(zoneId) }

    var selectedMonth by remember { mutableStateOf(YearMonth.now(zoneId)) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(today) }

    val events by eventRepository.observeEventList(EventQuery(cityId = cityId)).collectAsStateWithLifecycle(initialValue = emptyList())

    LaunchedEffect(cityId) {
        runCatching { eventRepository.refreshEventList(EventQuery(cityId = cityId)) }
    }

    val eventsByDay = remember(events, selectedMonth, zoneId) {
        events
            .filter { event ->
                val eventDate = event.startsAt.atZone(zoneId).toLocalDate()
                YearMonth.from(eventDate) == selectedMonth
            }
            .groupBy { it.startsAt.atZone(zoneId).toLocalDate() }
    }

    val gridDays = remember(selectedMonth) {
        val monthStart = selectedMonth.atDay(1)
        val monthEnd = selectedMonth.atEndOfMonth()
        val gridStart = monthStart.minusDays(
            if (monthStart.dayOfWeek == DayOfWeek.SUNDAY) 0L else monthStart.dayOfWeek.value.toLong(),
        )
        val gridEnd = monthEnd.plusDays(
            if (monthEnd.dayOfWeek == DayOfWeek.SATURDAY) 0L
            else (6 - monthEnd.dayOfWeek.value).toLong(),
        )
        buildList {
            var d = gridStart
            while (!d.isAfter(gridEnd)) {
                add(d)
                d = d.plusDays(1)
            }
        }
    }

    val dayEvents = selectedDay?.let { eventsByDay[it] } ?: emptyList()

    Column(
        verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
        modifier = Modifier
            .fillMaxSize()
            .padding(Tokens.Space.S4),
    ) {
        Text("Calendar", style = FamilyTypography.TitleLarge)

        if (onSetCity != null) {
            FilterChip(
                selected = cityName != null,
                onClick = onSetCity,
                label = { Text(cityName ?: "Select city") },
            )
        }

        // Month header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous month")
            }
            Text(
                text = "%s %d".format(
                    selectedMonth.month.getDisplayName(TextStyle.FULL, locale),
                    selectedMonth.year,
                ),
                style = FamilyTypography.TitleMedium,
            )
            IconButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next month")
            }
        }

        // Day-of-week labels
        Row(modifier = Modifier.fillMaxWidth()) {
            DAY_HEADERS.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = FamilyTypography.Caption,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }

        // Month grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false,
        ) {
            items(gridDays) { date ->
                val inMonth = date.month == selectedMonth.month && date.year == selectedMonth.year
                val isToday = date == today
                val isSelected = date == selectedDay
                val hasDot = eventsByDay.containsKey(date)

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .size(40.dp)
                        .then(if (!inMonth) Modifier.alpha(0.3f) else Modifier)
                        .padding(2.dp)
                        .then(
                            when {
                                isSelected -> Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                                isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else -> Modifier
                            },
                        )
                        .clickable { selectedDay = date },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = FamilyTypography.Caption,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.Unspecified,
                        )
                        if (hasDot) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary,
                                        CircleShape,
                                    ),
                            )
                        }
                    }
                }
            }
        }

        // Events for selected day
        if (dayEvents.isEmpty() && selectedDay != null) {
            EmptyState("No events on this day.")
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Tokens.Space.S3),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(dayEvents, key = { it.id.rawValue }) { event ->
                    EventCard(
                        title = decodeHtmlEntities(event.title),
                        subtitle = event.venueName ?: "Family event",
                        badge = event.tags.firstOrNull()?.label,
                        imageUrl = event.imageUrl,
                        onClick = { onOpenEvent(event.id) },
                    )
                }
            }
        }
    }
}
