package com.familyevents.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.familyevents.core.EventId

class PlatformActions(private val context: Context) {
    fun share(eventId: EventId) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "https://familyevents.app/share/${eventId.rawValue}")
        context.startActivity(Intent.createChooser(intent, "Share event").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun directions(query: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun addToCalendar(title: String, startMillis: Long, endMillis: Long?) {
        val intent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (endMillis != null) intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
        context.startActivity(intent)
    }
}

interface PushRegistration {
    suspend fun register(token: String)
}

class DeferredPushRegistration : PushRegistration {
    override suspend fun register(token: String) = Unit
}
