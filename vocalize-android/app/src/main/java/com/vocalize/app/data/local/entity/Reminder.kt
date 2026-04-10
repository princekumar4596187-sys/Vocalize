package com.vocalize.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = MemoEntity::class,
            parentColumns = ["id"],
            childColumns = ["memoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("memoId"), Index("reminderTime")]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val memoId: String,
    val reminderTime: Long,
    val repeatType: RepeatType = RepeatType.NONE,
    val customDays: String = ""
)
