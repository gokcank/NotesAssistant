package com.gokcank.notesassistant.calendar

import android.content.Intent
import android.provider.CalendarContract

object CalendarHelper {

    /** Takvim uygulamasında yeni etkinlik oluşturma ekranını açan intent. */
    fun insertEventIntent(title: String, description: String, startMillis: Long): Intent =
        Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 60L * 60 * 1000)
            .putExtra(CalendarContract.Events.TITLE, title)
            .putExtra(CalendarContract.Events.DESCRIPTION, description)
}
