package com.gokcank.notesassistant

import android.app.Application
import android.content.Context
import com.gokcank.notesassistant.data.AppDatabase
import com.gokcank.notesassistant.data.BackupManager
import com.gokcank.notesassistant.data.DriveSyncManager
import com.gokcank.notesassistant.data.NotesRepository
import com.gokcank.notesassistant.data.SettingsStore
import com.google.android.gms.ads.MobileAds
import kotlin.concurrent.thread

class AppContainer(context: Context) {
    val database = AppDatabase.get(context)
    val repository = NotesRepository(database.noteDao())
    val backupManager = BackupManager(context, database)
    val settingsStore = SettingsStore(context)
    val driveSync = DriveSyncManager(context, database, settingsStore)
}

class NotesApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        thread { MobileAds.initialize(this) }
    }
}
