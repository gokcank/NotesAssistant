package com.gokcank.notesassistant.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val isChecklist: Boolean = false,
    val isPinned: Boolean = false,
    /** ui/NoteColors.kt paletindeki renk dizini; 0 = varsayılan */
    val color: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("noteId")],
)
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val text: String = "",
    val isDone: Boolean = false,
    val position: Int = 0,
)

@Serializable
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("noteId")],
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long? = null,
    val title: String,
    val message: String = "",
    val triggerAt: Long,
)

data class NoteWithItems(
    @Embedded val note: Note,
    @Relation(parentColumn = "id", entityColumn = "noteId")
    val items: List<ChecklistItem>,
)
