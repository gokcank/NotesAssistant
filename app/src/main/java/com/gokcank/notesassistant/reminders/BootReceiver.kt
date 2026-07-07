package com.gokcank.notesassistant.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gokcank.notesassistant.NotesApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Cihaz yeniden başlatıldığında gelecekteki hatırlatıcı alarmlarını yeniden kurar. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val app = context.applicationContext as? NotesApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val upcoming = app.container.repository.upcomingReminders(System.currentTimeMillis())
                upcoming.forEach { app.container.scheduler.schedule(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
