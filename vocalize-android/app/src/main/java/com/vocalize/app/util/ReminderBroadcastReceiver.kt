package com.vocalize.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vocalize.app.data.local.entity.ReminderEntity
import com.vocalize.app.data.local.entity.RepeatType
import com.vocalize.app.data.repository.MemoRepository
import com.vocalize.app.service.PlaybackService
import com.vocalize.app.service.ReminderToneService
import dagger.hilt.android.AndroidEntryPoint
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vocalize.app.dataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ReminderBroadcastReceiver : BroadcastReceiver() {

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: ReminderAlarmScheduler
    @Inject lateinit var memoRepository: MemoRepository

    override fun onReceive(context: Context, intent: Intent) {
        val memoId = intent.getStringExtra(Constants.EXTRA_MEMO_ID) ?: return
        val memoTitle = intent.getStringExtra(Constants.EXTRA_MEMO_TITLE) ?: "Voice Memo"
        val reminderId = intent.getStringExtra(Constants.EXTRA_REMINDER_ID)
        val pendingResult = goAsync()

        when (intent.action) {
            Constants.ACTION_PLAY -> {
                val serviceIntent = Intent(context, ReminderToneService::class.java).apply {
                    action = ReminderToneService.ACTION_START_REMINDER
                    putExtra(Constants.EXTRA_MEMO_ID, memoId)
                    putExtra(Constants.EXTRA_MEMO_TITLE, memoTitle)
                    reminderId?.let { putExtra(Constants.EXTRA_REMINDER_ID, it) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                pendingResult.finish()
                return
            }
            Constants.ACTION_SHOW_NOTE -> {
                notificationHelper.showReminderNoteNotification(memoId, memoTitle, reminderId)
                context.stopService(Intent(context, ReminderToneService::class.java))
            }
            Constants.ACTION_BACK_TO_REMINDER -> {
                notificationHelper.showReminderNotification(memoId, memoTitle, reminderId)
            }
            Constants.ACTION_REMINDER_PLAY -> {
                val playbackIntent = Intent(context, PlaybackService::class.java).apply {
                    action = Constants.ACTION_PLAY_AUDIO
                    putExtra(Constants.EXTRA_MEMO_ID, memoId)
                    putExtra(Constants.EXTRA_MEMO_TITLE, memoTitle)
                    putExtra(Constants.EXTRA_NOTIFICATION_ID, memoId.hashCode())
                    reminderId?.let { putExtra(Constants.EXTRA_REMINDER_ID, it) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(playbackIntent)
                } else {
                    context.startService(playbackIntent)
                }
                context.stopService(Intent(context, ReminderToneService::class.java))
            }
            Constants.ACTION_SNOOZE -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        handleSnoozeAction(context, memoId, memoTitle, reminderId)
                    } finally {
                        pendingResult.finish()
                    }
                }
                notificationHelper.cancelNotification(memoId)
                context.stopService(Intent(context, ReminderToneService::class.java))
                return
            }
            Constants.ACTION_DISMISS -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        handleDismissAction(memoId, memoTitle, reminderId)
                    } finally {
                        pendingResult.finish()
                    }
                }
                notificationHelper.cancelNotification(memoId)
                context.stopService(Intent(context, ReminderToneService::class.java))
                return
            }
        }

        pendingResult.finish()
    }

    private suspend fun handleSnoozeAction(context: Context, memoId: String, memoTitle: String, reminderId: String?) {
        val snoozeMinutes = context.dataStore.data.first()[stringPreferencesKey(Constants.PREFS_DEFAULT_SNOOZE)]?.toIntOrNull() ?: 10
        val snoozeTime = System.currentTimeMillis() + snoozeMinutes * 60 * 1000L

        val reminder = reminderId?.let { memoRepository.getReminderById(it) }

        if (reminder != null) {
            memoRepository.updateReminderEntry(reminder.id, snoozeTime, reminder.repeatType, reminder.customDays)
            memoRepository.updateReminder(memoId, true, snoozeTime, reminder.repeatType, reminder.customDays)
            alarmScheduler.scheduleReminder(reminder.copy(reminderTime = snoozeTime), memoTitle)
        } else {
            val memo = memoRepository.getMemoById(memoId) ?: return
            memoRepository.updateReminder(memoId, true, snoozeTime, memo.repeatType, memo.customDays)
            alarmScheduler.scheduleReminder(memo.copy(reminderTime = snoozeTime))
        }
    }

    private suspend fun handleDismissAction(memoId: String, memoTitle: String, reminderId: String?) {
        if (reminderId != null) {
            val reminder = memoRepository.getReminderById(reminderId)
            if (reminder != null) {
                if (reminder.repeatType == RepeatType.NONE) {
                    memoRepository.deleteReminderById(reminderId)
                } else {
                    val nextTime = calculateNextRepeatTime(reminder)
                    if (nextTime != null) {
                        memoRepository.updateReminderEntry(reminderId, nextTime, reminder.repeatType, reminder.customDays)
                    } else {
                        memoRepository.deleteReminderById(reminderId)
                    }
                }
            }
        }

        val nextReminder = memoRepository.getRemindersForMemo(memoId)
            .first()
            .filter { it.reminderTime > System.currentTimeMillis() }
            .minByOrNull { it.reminderTime }

        if (nextReminder != null) {
            memoRepository.updateReminder(memoId, true, nextReminder.reminderTime, nextReminder.repeatType, nextReminder.customDays)
            alarmScheduler.scheduleReminder(nextReminder, memoTitle)
        } else {
            memoRepository.updateReminder(memoId, false, null, RepeatType.NONE, "")
        }
    }

    private fun calculateNextRepeatTime(reminder: ReminderEntity): Long? {
        val now = System.currentTimeMillis()
        return when (reminder.repeatType) {
            RepeatType.DAILY -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = reminder.reminderTime
                    while (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            RepeatType.WEEKLY -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = reminder.reminderTime
                    while (timeInMillis <= now) add(Calendar.WEEK_OF_YEAR, 1)
                }
                cal.timeInMillis
            }
            RepeatType.CUSTOM_DAYS -> {
                val days = reminder.customDays.split(",").mapNotNull { it.trim().toIntOrNull() }
                if (days.isEmpty()) null else {
                    val cal = Calendar.getInstance()
                    val currentDay = cal.get(Calendar.DAY_OF_WEEK)
                    val nextDay = days.firstOrNull { it > currentDay } ?: days.first()
                    val daysUntil = if (nextDay > currentDay) nextDay - currentDay else 7 - currentDay + nextDay
                    cal.apply {
                        timeInMillis = reminder.reminderTime
                        add(Calendar.DAY_OF_YEAR, daysUntil)
                    }.timeInMillis
                }
            }
            RepeatType.NONE -> null
        }
    }
}
