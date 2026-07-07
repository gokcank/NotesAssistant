package com.gokcank.notesassistant

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.gokcank.notesassistant.data.AppDatabase
import com.gokcank.notesassistant.data.BackupManager
import com.gokcank.notesassistant.data.NotesRepository
import com.gokcank.notesassistant.data.SettingsStore
import com.gokcank.notesassistant.reminders.ReminderScheduler
import com.google.android.gms.ads.MobileAds
import kotlin.concurrent.thread

class AppContainer(context: Context) {
    val database = AppDatabase.get(context)
    val repository = NotesRepository(database.noteDao(), database.reminderDao())
    val scheduler = ReminderScheduler(context)
    val backupManager = BackupManager(context, database, scheduler)
    val settingsStore = SettingsStore(context)
}

class NotesApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.reminder_channel_desc)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)

        thread { MobileAds.initialize(this) }
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "reminders"
    }
}
