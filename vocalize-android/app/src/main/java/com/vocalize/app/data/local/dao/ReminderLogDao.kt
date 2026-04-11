package com.vocalize.app.data.local.dao

import androidx.room.*
import com.vocalize.app.data.local.entity.ReminderLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ReminderLogEntity)

    @Query("SELECT * FROM reminder_logs WHERE reminderId = :reminderId ORDER BY firedTime DESC LIMIT 1")
    suspend fun getLatestLogForReminder(reminderId: String): ReminderLogEntity?

    @Query("SELECT * FROM reminder_logs ORDER BY firedTime DESC")
    fun getAllLogs(): Flow<List<ReminderLogEntity>>

    @Query("DELETE FROM reminder_logs WHERE firedTime < :before")
    suspend fun deleteOldLogs(before: Long)
}
