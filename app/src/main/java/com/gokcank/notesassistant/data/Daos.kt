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
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun observeNotes(): Flow<List<NoteWithItems>>

    @Transaction
    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<NoteWithItems>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNote(id: Long): NoteWithItems?

    // Yedeğe yalnızca çöpte olmayan notlar girer
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL")
    suspend fun getAllNotes(): List<Note>

    @Query("SELECT * FROM checklist_items WHERE noteId IN (SELECT id FROM notes WHERE deletedAt IS NULL)")
    suspend fun getAllItems(): List<ChecklistItem>

    @Query("UPDATE notes SET deletedAt = :at WHERE id = :id")
    suspend fun moveToTrash(id: Long, at: Long)

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL")
    suspend fun emptyTrash()

    @Query("DELETE FROM notes WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeTrash(cutoff: Long)

    // Drive eşitlemesi: çöptekiler dahil tüm kayıtlar (silinme bilgisi diğer cihaza taşınır)
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesForSync(): List<Note>

    @Query("SELECT * FROM checklist_items")
    suspend fun getAllItemsForSync(): List<ChecklistItem>

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
