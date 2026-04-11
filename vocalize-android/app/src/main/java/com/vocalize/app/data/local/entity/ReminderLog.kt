package com.vocalize.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminder_logs")
data class ReminderLogEntity(
    @PrimaryKey val id: String,
    val reminderId: String,
    val memoId: String,
    val memoTitle: String,
    val scheduledTime: Long,
    val firedTime: Long,
    val diagnostics: String
)
