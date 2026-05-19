package com.familyevents.eventdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import com.familyevents.core.DateFormatting
import com.familyevents.core.EventId
import com.familyevents.core.decodeHtmlEntities
import com.familyevents.data.EventDto
import com.familyevents.data.EventRepository
import com.familyevents.designsystem.EmptyState
import com.familyevents.designsystem.EventHeroImage
import com.familyevents.designsystem.FamilyTypography
import com.familyevents.designsystem.LoadingState
import com.familyevents.designsystem.TagPill
import com.familyevents.designsystem.generated.Tokens

private fun text(s: String?): String = decodeHtmlEntities(s ?: "")

@Composable
fun PublicSharePreviewScreen(
    eventId: EventId,
    eventRepository: EventRepository,
    onSignIn: () -> Unit,
    onLearnMore: () -> Unit = {},
) {
    var event by remember { mutableStateOf<EventDto?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(eventId.rawValue) {
        loading = true
        event = runCatching { eventRepository.publicEvent(eventId) }.getOrNull()
        loading = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            loading -> LoadingState()
            event == null -> EmptyState(
                title = "Event not found",
                actionLabel = "Sign in",
                onAction = onSignIn,
            )
            else -> {
                val current = event!!
                Column(
                    verticalArrangement = Arrangement.spacedBy(Tokens.Space.S4),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Tokens.Space.S4),
                ) {
                    EventHeroImage(
                        title = text(current.title),
                        imageUrl = current.imageUrl,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(Tokens.Space.S2)) {
                        TagPill(label = "Shared plan")
                    }

                    Text(
                        text = text(current.title),
                        style = FamilyTypography.TitleLarge,
                    )

                    Text(
                        text = DateFormatting.cardSubtitle(current.startsAt),
                        style = FamilyTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )

                    val venue = text(current.venueName)
                    if (venue.isNotEmpty()) {
                        Text(
                            text = venue,
                            style = FamilyTypography.Body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }

                    val description = text(current.description)
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            style = FamilyTypography.Body,
                        )
                    }

                    Button(
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open in Family Events")
                    }
                }
            }
        }
    }
}
