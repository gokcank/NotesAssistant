package com.gokcank.notesassistant.data

import android.content.Context
import android.net.Uri
import com.gokcank.notesassistant.R
import com.gokcank.notesassistant.reminders.ReminderScheduler
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long,
    val notes: List<Note>,
    val items: List<ChecklistItem>,
    val reminders: List<Reminder>,
)

/**
 * Manuel JSON yedekleme (SAF üzerinden dışa/içe aktarma).
 * Otomatik bulut yedeği ayrıca Android Auto Backup ile yapılır (backup_rules.xml).
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val scheduler: ReminderScheduler,
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun exportTo(uri: Uri) {
        val noteDao = database.noteDao()
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            notes = noteDao.getAllNotes(),
            items = noteDao.getAllItems(),
            reminders = database.reminderDao().getUpcoming(0),
        )
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(json.encodeToString(BackupData.serializer(), data).toByteArray())
        } ?: error(context.getString(R.string.error_file_write))
    }

    /** Yedekteki kayıtları yeni kayıtlar olarak ekler (mevcut veriyi silmez). Eklenen not sayısını döndürür. */
    suspend fun importFrom(uri: Uri): Int {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.readBytes().decodeToString()
        } ?: error(context.getString(R.string.error_file_read))
        val data = json.decodeFromString(BackupData.serializer(), text)

        val noteDao = database.noteDao()
        val reminderDao = database.reminderDao()
        val idMap = mutableMapOf<Long, Long>()
        for (note in data.notes) {
            val newId = noteDao.insertNote(note.copy(id = 0))
            idMap[note.id] = newId
        }
        val items = data.items.mapNotNull { item ->
            idMap[item.noteId]?.let { item.copy(id = 0, noteId = it) }
        }
        if (items.isNotEmpty()) noteDao.insertItems(items)

        val now = System.currentTimeMillis()
        data.reminders.filter { it.triggerAt > now }.forEach { reminder ->
            val remapped = reminder.copy(id = 0, noteId = reminder.noteId?.let { idMap[it] })
            val id = reminderDao.insert(remapped)
            scheduler.schedule(remapped.copy(id = id))
        }
        return data.notes.size
    }
}
