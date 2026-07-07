package com.gokcank.notesassistant.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Transaction
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteWithItems>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: Long): NoteWithItems?

    @Query("SELECT * FROM notes")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllItems(): List<ChecklistItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItem>)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteItemsForNote(noteId: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerAt ASC")
    fun observeReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE triggerAt > :now")
    suspend fun getUpcoming(now: Long): List<Reminder>

    @Query("SELECT * FROM reminders WHERE noteId = :noteId")
    suspend fun getForNote(noteId: Long): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: Reminder): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)
}
