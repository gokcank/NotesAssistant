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
import com.gokcank.notesassistant.data.Reminder
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
    private val scheduler = container.scheduler
    private val backupManager = container.backupManager
    private val settingsStore = container.settingsStore

    val notes: StateFlow<List<NoteWithItems>> = repository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reminders: StateFlow<List<Reminder>> = repository.observeReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val themeMode: StateFlow<ThemeMode> = settingsStore.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    /** İlk açılış tanıtımı gösterildi mi? Başlangıçta true: diyalog ancak gerçek değer false gelirse açılır. */
    val onboardingDone: StateFlow<Boolean> = settingsStore.onboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun completeOnboarding() {
        viewModelScope.launch { settingsStore.setOnboardingDone() }
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

    fun saveNote(note: Note, items: List<ChecklistItem>, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveNote(note, items)
            onSaved(id)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.deleteNote(id) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.setPinned(note.id, !note.isPinned) }
    }

    private data class DeletedBundle(val note: NoteWithItems, val reminders: List<Reminder>)

    private var lastDeleted: DeletedBundle? = null

    /** "Geri Al" eylemli snackbar için tek seferlik mesaj. */
    private val _undoMessage = MutableStateFlow<String?>(null)
    val undoMessage: StateFlow<String?> = _undoMessage
    fun consumeUndoMessage() { _undoMessage.value = null }

    /** Notu siler ama geri alınabilmesi için bellekte tutar. */
    fun deleteNoteWithUndo(id: Long) {
        viewModelScope.launch {
            val note = repository.getNote(id) ?: return@launch
            val reminders = repository.remindersForNote(id)
            reminders.forEach { scheduler.cancel(it.id) }
            repository.deleteNote(id)
            lastDeleted = DeletedBundle(note, reminders)
            _undoMessage.value = getString(R.string.msg_note_deleted)
        }
    }

    fun undoDelete() {
        val deleted = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            repository.restoreNote(deleted.note, deleted.reminders)
            val now = System.currentTimeMillis()
            deleted.reminders.filter { it.triggerAt > now }.forEach { scheduler.schedule(it) }
        }
    }

    fun addReminder(noteId: Long?, title: String, message: String, triggerAt: Long) {
        viewModelScope.launch {
            val saved = repository.addReminder(
                Reminder(noteId = noteId, title = title, message = message, triggerAt = triggerAt)
            )
            scheduler.schedule(saved)
            _message.value = getString(R.string.msg_reminder_set)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            scheduler.cancel(reminder.id)
            repository.deleteReminder(reminder.id)
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
