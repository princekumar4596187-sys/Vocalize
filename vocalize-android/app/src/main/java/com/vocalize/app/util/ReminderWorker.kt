package com.vocalize.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.vocalize.app.R
import com.vocalize.app.VocalizeApplication
import com.vocalize.app.data.local.entity.RepeatType
import com.vocalize.app.data.repository.MemoRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val memoRepository: MemoRepository,
    private val alarmScheduler: ReminderAlarmScheduler
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val memoId = inputData.getString(Constants.EXTRA_MEMO_ID) ?: return Result.failure()
        val memoTitle = inputData.getString(Constants.EXTRA_MEMO_TITLE) ?: "Voice Memo"
        val reminderId = inputData.getString(Constants.EXTRA_REMINDER_ID)

        // Handle repeat scheduling and DB cleanup
        if (reminderId != null) {
            val reminder = memoRepository.getReminderById(reminderId)
            if (reminder != null) {
                if (reminder.repeatType != RepeatType.NONE) {
                    alarmScheduler.scheduleNextRepeat(reminder, memoTitle)
                } else {
                    memoRepository.deleteReminderById(reminder.id)
                }
            }
        }

        refreshMemoReminderFields(memoId)
        return Result.success()
    }

    // Required by WorkManager for expedited workers on API < 31 (uses foreground service).
    // Uses a worker-specific notification ID that does NOT conflict with ReminderToneService.
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val memoId = inputData.getString(Constants.EXTRA_MEMO_ID) ?: "worker"
        val workerNotifId = "reminder_db_work_$memoId".hashCode()
        val notification = buildWorkerNotification()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(workerNotifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(workerNotifId, notification)
        }
    }

    private fun buildWorkerNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = appContext.getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(WORKER_CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    WORKER_CHANNEL_ID,
                    "Reminder Scheduler",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Used internally to schedule reminders reliably"
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
        return NotificationCompat.Builder(appContext, WORKER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Scheduling reminder...")
            .setContentText("Updating reminder schedule")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false)
            .setSilent(true)
            .build()
    }

    private suspend fun refreshMemoReminderFields(memoId: String) {
        val reminders = memoRepository.getRemindersForMemo(memoId)
            .first()
            .filter { it.reminderTime > System.currentTimeMillis() }

        if (reminders.isEmpty()) {
            memoRepository.updateReminder(memoId, false, null, RepeatType.NONE, "")
        } else {
            val nextReminder = reminders.minByOrNull { it.reminderTime }!!
            memoRepository.updateReminder(
                memoId,
                true,
                nextReminder.reminderTime,
                nextReminder.repeatType,
                nextReminder.customDays
            )
        }
    }

    companion object {
        const val WORK_TAG = "reminder_db_worker"
        private const val WORKER_CHANNEL_ID = "vocalize_reminder_worker"

        fun enqueueDbWork(context: Context, memoId: String, memoTitle: String, reminderId: String?) {
            val data = workDataOf(
                Constants.EXTRA_MEMO_ID to memoId,
                Constants.EXTRA_MEMO_TITLE to memoTitle,
                Constants.EXTRA_REMINDER_ID to reminderId
            )
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(WORK_TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminder_db_${reminderId ?: memoId}",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
