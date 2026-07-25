package com.gokcank.notesassistant.data

import kotlinx.coroutines.flow.Flow

class NotesRepository(private val noteDao: NoteDao) {
    fun observeNotes(): Flow<List<NoteWithItems>> = noteDao.observeNotes()

    suspend fun getNote(id: Long): NoteWithItems? = noteDao.getNote(id)

    /** Notu ve (checklist ise) maddelerini kaydeder; not kimliğini döndürür. */
    suspend fun saveNote(note: Note, items: List<ChecklistItem>): Long {
        val now = System.currentTimeMillis()
        val id = if (note.id == 0L) {
            noteDao.insertNote(note.copy(createdAt = now, updatedAt = now))
        } else {
            noteDao.updateNote(note.copy(updatedAt = now))
            note.id
        }
        noteDao.deleteItemsForNote(id)
        if (items.isNotEmpty()) {
            noteDao.insertItems(
                items.mapIndexed { index, item ->
                    item.copy(id = 0, noteId = id, position = index)
                }
            )
        }
        return id
    }

    suspend fun setPinned(id: Long, pinned: Boolean) = noteDao.setPinned(id, pinned)

    // --- Çöp kutusu ---

    fun observeTrash(): Flow<List<NoteWithItems>> = noteDao.observeTrash()

    suspend fun moveToTrash(id: Long) = noteDao.moveToTrash(id, System.currentTimeMillis())

    suspend fun restoreFromTrash(id: Long) = noteDao.restoreFromTrash(id)

    /** Notu kalıcı olarak siler (çöp ekranından). */
    suspend fun deleteForever(id: Long) = noteDao.deleteNote(id)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    /** Çöpte [TRASH_RETENTION_MS] süresinden uzun bekleyenleri kalıcı siler. */
    suspend fun purgeOldTrash() =
        noteDao.purgeTrash(System.currentTimeMillis() - TRASH_RETENTION_MS)

    companion object {
        /** Çöpte bekletme süresi: 30 gün */
        const val TRASH_RETENTION_MS = 30L * 24 * 60 * 60 * 1000
    }
}
