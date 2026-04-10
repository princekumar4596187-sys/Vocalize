package com.vocalize.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vocalize.app.data.repository.MemoRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var memoRepository: MemoRepository
    @Inject lateinit var alarmScheduler: ReminderAlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val now = System.currentTimeMillis()
                memoRepository.getAllMemosWithReminders().forEach { memo ->
                    if ((memo.reminderTime ?: 0L) > now) {
                        alarmScheduler.scheduleReminder(memo)
                    }
                }
            }
        }
    }
}
