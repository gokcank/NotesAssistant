package com.gokcank.notesassistant.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import kotlinx.serialization.Serializable
import java.util.UUID

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
    /** Dolu ise not çöp kutusundadır; değer çöpe atılma anıdır. */
    val deletedAt: Long? = null,
    /** Kullanıcının serbest yazdığı tek kategori etiketi (İş, Ev, Alışveriş…). */
    val label: String? = null,
    /** Gizli not: içerik önizlemesi saklanır, açmak biyometrik doğrulama ister. */
    val isLocked: Boolean = false,
    /**
     * Cihazlar arası eşitleme kimliği: yerel `id` her cihazda farklı üretildiğinden
     * Drive eşitlemesi notları bu değişmez kimlikle eşleştirir.
     */
    val syncId: String = UUID.randomUUID().toString(),
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

data class NoteWithItems(
    @Embedded val note: Note,
    @Relation(parentColumn = "id", entityColumn = "noteId")
    val items: List<ChecklistItem>,
)
