package com.gokcank.notesassistant.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.gokcank.notesassistant.MainActivity
import com.gokcank.notesassistant.NotesApp
import com.gokcank.notesassistant.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val id = intent.getLongExtra(EXTRA_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.app_name)
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()
        val noteId = if (intent.hasExtra(EXTRA_NOTE_ID)) intent.getLongExtra(EXTRA_NOTE_ID, -1) else -1

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (noteId > 0) putExtra(MainActivity.EXTRA_OPEN_NOTE_ID, noteId)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = Notification.Builder(context, NotesApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message.ifBlank { title })
            .setStyle(Notification.BigTextStyle().bigText(message.ifBlank { title }))
            .setContentIntent(contentPi)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(NotificationManager::class.java)
        nm?.notify(id.toInt(), notification)
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_MESSAGE = "reminder_message"
        const val EXTRA_NOTE_ID = "reminder_note_id"
    }
}
