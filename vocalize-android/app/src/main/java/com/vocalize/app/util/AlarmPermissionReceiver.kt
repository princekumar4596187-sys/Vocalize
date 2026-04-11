package com.vocalize.app.util

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.vocalize.app.data.repository.MemoRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmPermissionReceiver : BroadcastReceiver() {

    @Inject lateinit var memoRepository: MemoRepository
    @Inject lateinit var alarmScheduler: ReminderAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val shouldReschedule = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                alarmManager?.canScheduleExactAlarms() == true
            }
            action == Intent.ACTION_BOOT_COMPLETED ||
                    action == "android.intent.action.LOCKED_BOOT_COMPLETED" ||
                    action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    action == Intent.ACTION_TIMEZONE_CHANGED ||
                    action == Intent.ACTION_TIME_CHANGED -> true
            else -> false
        }

        if (shouldReschedule) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    memoRepository.getAllMemosWithReminders().forEach { memo ->
                        alarmScheduler.scheduleReminder(memo)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
