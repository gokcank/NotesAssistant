package com.gokcank.notesassistant.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.gokcank.notesassistant.data.Reminder

class ReminderScheduler(private val context: Context) {

    fun schedule(reminder: Reminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = pendingIntent(reminder.id) {
            putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, reminder.message)
            reminder.noteId?.let { putExtra(ReminderReceiver.EXTRA_NOTE_ID, it) }
        }
        val canExact = Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAt, pi)
        }
    }

    fun cancel(reminderId: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(pendingIntent(reminderId) {})
    }

    private fun pendingIntent(reminderId: Long, configure: Intent.() -> Unit): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply(configure)
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
