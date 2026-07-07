package com.gokcank.notesassistant.data

import kotlinx.coroutines.flow.Flow

class NotesRepository(
    private val noteDao: NoteDao,
    private val reminderDao: ReminderDao,
) {
    fun observeNotes(): Flow<List<NoteWithItems>> = noteDao.observeNotes()
    fun observeReminders(): Flow<List<Reminder>> = reminderDao.observeReminders()

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

    suspend fun deleteNote(id: Long) = noteDao.deleteNote(id)

    suspend fun setPinned(id: Long, pinned: Boolean) = noteDao.setPinned(id, pinned)

    suspend fun remindersForNote(noteId: Long): List<Reminder> = reminderDao.getForNote(noteId)

    /** Silinen bir notu maddeleri ve hatırlatıcılarıyla, özgün kimlikleriyle geri getirir. */
    suspend fun restoreNote(noteWithItems: NoteWithItems, reminders: List<Reminder>) {
        noteDao.insertNote(noteWithItems.note)
        if (noteWithItems.items.isNotEmpty()) noteDao.insertItems(noteWithItems.items)
        reminders.forEach { reminderDao.insert(it) }
    }

    suspend fun addReminder(reminder: Reminder): Reminder {
        val id = reminderDao.insert(reminder)
        return reminder.copy(id = id)
    }

    suspend fun deleteReminder(id: Long) = reminderDao.delete(id)

    suspend fun upcomingReminders(now: Long): List<Reminder> = reminderDao.getUpcoming(now)
}
