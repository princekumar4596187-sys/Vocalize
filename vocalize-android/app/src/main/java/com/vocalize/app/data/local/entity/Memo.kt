package com.vocalize.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memos",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class MemoEntity(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val duration: Long,
    val dateCreated: Long,
    val dateModified: Long,
    val hasReminder: Boolean = false,
    val reminderTime: Long? = null,
    val repeatType: RepeatType = RepeatType.NONE,
    val customDays: String = "",
    val categoryId: String? = null,
    val textNote: String = "",
    val transcription: String = "",
    val isTranscribing: Boolean = false
)

enum class RepeatType {
    NONE, DAILY, WEEKLY, CUSTOM_DAYS
}
