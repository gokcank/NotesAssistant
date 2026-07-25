package com.gokcank.notesassistant.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gokcank.notesassistant.NotesApp
import com.gokcank.notesassistant.R
import com.gokcank.notesassistant.data.ChecklistItem
import com.gokcank.notesassistant.data.Note
import com.gokcank.notesassistant.data.NoteWithItems
import com.gokcank.notesassistant.data.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as NotesApp).container
    private val repository = container.repository
    private val backupManager = container.backupManager
    private val settingsStore = container.settingsStore
    private val driveSync = container.driveSync

    init {
        viewModelScope.launch {
            // Çöpte 30 günü dolan notlar açılışta sessizce temizlenir
            repository.purgeOldTrash()
            // Açılışta bir eşitleme turu (bağlı değilse sessizce atlanır)
            driveSync.syncNow()
        }
    }

    val notes: StateFlow<List<NoteWithItems>> = repository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trash: StateFlow<List<NoteWithItems>> = repository.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    val gridView: StateFlow<Boolean> = settingsStore.gridView
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun toggleGridView() {
        viewModelScope.launch { settingsStore.setGridView(!gridView.value) }
    }

    /** Snackbar'da gösterilecek tek seferlik mesaj. */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun consumeMessage() { _message.value = null }

    private fun getString(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    suspend fun getNote(id: Long): NoteWithItems? = repository.getNote(id)

    /** Not değişikliklerinden kısa süre sonra Drive'a sessiz eşitleme. */
    private var pendingSync: kotlinx.coroutines.Job? = null
    private fun scheduleSync() {
        pendingSync?.cancel()
        pendingSync = viewModelScope.launch {
            kotlinx.coroutines.delay(4_000)
            driveSync.syncNow()
        }
    }

    fun saveNote(note: Note, items: List<ChecklistItem>, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveNote(note, items)
            onSaved(id)
            scheduleSync()
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.setPinned(note.id, !note.isPinned) }
    }

    private var lastDeletedId: Long? = null

    /** "Geri Al" eylemli snackbar için tek seferlik mesaj. */
    private val _undoMessage = MutableStateFlow<String?>(null)
    val undoMessage: StateFlow<String?> = _undoMessage
    fun consumeUndoMessage() { _undoMessage.value = null }

    /** Notu çöp kutusuna taşır; "Geri Al" ile anında geri getirilebilir. */
    fun deleteNoteWithUndo(id: Long) {
        viewModelScope.launch {
            repository.moveToTrash(id)
            lastDeletedId = id
            _undoMessage.value = getString(R.string.msg_note_deleted)
            scheduleSync()
        }
    }

    fun undoDelete() {
        val id = lastDeletedId ?: return
        lastDeletedId = null
        viewModelScope.launch {
            repository.restoreFromTrash(id)
            scheduleSync()
        }
    }

    // --- Çöp kutusu ---

    fun restoreFromTrash(id: Long) {
        viewModelScope.launch {
            repository.restoreFromTrash(id)
            scheduleSync()
        }
    }

    fun deleteForever(id: Long) {
        viewModelScope.launch {
            repository.deleteForever(id)
            scheduleSync()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            scheduleSync()
        }
    }

    // --- Google Drive eşitleme ---

    val driveSyncEnabled: StateFlow<Boolean> = settingsStore.driveSyncEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val lastSyncAt: StateFlow<Long> = settingsStore.lastSyncAt
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    /** İlk bağlanma akışını başlatır; onay ekranı gerekiyorsa [onResolution] tetiklenir. */
    fun beginDriveAuthorization(onResolution: (android.app.PendingIntent) -> Unit) {
        driveSync.requestAuthorization(
            onGranted = { completeDriveConnection() },
            onResolution = onResolution,
            onError = { _message.value = getString(R.string.msg_drive_connect_failed) },
        )
    }

    /** İzin verildikten sonra çağrılır: eşitlemeyi açar ve ilk turu koşar. */
    fun completeDriveConnection() {
        viewModelScope.launch {
            settingsStore.setDriveSyncEnabled(true)
            syncNow()
        }
    }

    fun disconnectDrive() {
        viewModelScope.launch { settingsStore.setDriveSyncEnabled(false) }
    }

    /** Elle tetiklenen eşitleme; sonucu kullanıcıya bildirir. */
    fun syncNow() {
        viewModelScope.launch {
            _message.value =
                if (driveSync.syncNow()) getString(R.string.msg_sync_done)
                else getString(R.string.msg_sync_failed)
        }
    }

    fun createNoteFromText(title: String, body: String, onCreated: (Long) -> Unit) {
        saveNote(Note(title = title, body = body), emptyList(), onCreated)
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { backupManager.exportTo(uri) } }
                .onSuccess { _message.value = getString(R.string.msg_export_done) }
                .onFailure { _message.value = getString(R.string.msg_export_failed, it.message.orEmpty()) }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { backupManager.importFrom(uri) } }
                .onSuccess { _message.value = getString(R.string.msg_import_done, it) }
                .onFailure { _message.value = getString(R.string.msg_import_failed, it.message.orEmpty()) }
        }
    }

    /** Metin tabanlı bir belgeyi (txt, md vb.) yeni bir nota dönüştürür. */
    fun importDocument(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val context = getApplication<Application>()
                    val text = context.contentResolver.openInputStream(uri)?.use {
                        it.readBytes().decodeToString()
                    } ?: error(getString(R.string.error_file_read))
                    val name = context.contentResolver.query(
                        uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: getString(R.string.imported_doc_title)
                    repository.saveNote(Note(title = name, body = text), emptyList())
                }
            }
                .onSuccess { _message.value = getString(R.string.msg_doc_imported) }
                .onFailure { _message.value = getString(R.string.msg_doc_import_failed, it.message.orEmpty()) }
        }
    }
}
