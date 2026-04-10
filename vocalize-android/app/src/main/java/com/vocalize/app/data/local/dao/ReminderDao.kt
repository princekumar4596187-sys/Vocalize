package com.vocalize.app.data.local.dao

import androidx.room.*
import com.vocalize.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE memoId = :memoId ORDER BY reminderTime ASC")
    fun getRemindersForMemo(memoId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE reminderTime >= :now ORDER BY reminderTime ASC")
    fun getUpcomingReminders(now: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE reminderTime BETWEEN :start AND :end ORDER BY reminderTime ASC")
    fun getRemindersByDate(start: Long, end: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders")
    suspend fun getAllReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET reminderTime = :reminderTime, repeatType = :repeatType, customDays = :customDays WHERE id = :id")
    suspend fun updateReminder(id: String, reminderTime: Long, repeatType: String, customDays: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("DELETE FROM reminders WHERE memoId = :memoId")
    suspend fun deleteRemindersByMemo(memoId: String)
}
