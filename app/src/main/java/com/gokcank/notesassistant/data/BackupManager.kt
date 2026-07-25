package com.gokcank.notesassistant.data

import android.content.Context
import android.net.Uri
import com.gokcank.notesassistant.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: Long,
    val notes: List<Note>,
    val items: List<ChecklistItem>,
)

/**
 * Manuel JSON yedekleme (SAF üzerinden dışa/içe aktarma).
 * Otomatik bulut yedeği ayrıca Android Auto Backup ile yapılır (backup_rules.xml).
 */
class BackupManager(
    private val context: Context,
    private val database: AppDatabase,
) {
    // ignoreUnknownKeys: hatırlatıcı içeren eski yedek dosyaları da sorunsuz içe aktarılır
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun exportTo(uri: Uri) {
        val noteDao = database.noteDao()
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            notes = noteDao.getAllNotes(),
            items = noteDao.getAllItems(),
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
        val idMap = mutableMapOf<Long, Long>()
        for (note in data.notes) {
            // Yeni eşitleme kimliği üretilir: aynı yedek iki kez yüklenirse
            // kopyalar Drive eşitlemesinde birbirine karışmasın
            val newId = noteDao.insertNote(
                note.copy(id = 0, syncId = UUID.randomUUID().toString())
            )
            idMap[note.id] = newId
        }
        val items = data.items.mapNotNull { item ->
            idMap[item.noteId]?.let { item.copy(id = 0, noteId = it) }
        }
        if (items.isNotEmpty()) noteDao.insertItems(items)
        return data.notes.size
    }
}
