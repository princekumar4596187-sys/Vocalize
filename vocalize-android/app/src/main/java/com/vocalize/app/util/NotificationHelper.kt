package com.vocalize.app.util

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.vocalize.app.MainActivity
import com.vocalize.app.R
import com.vocalize.app.VocalizeApplication
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    fun showReminderNotification(memoId: String, memoTitle: String) {
        val notifId = memoId.hashCode()

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Constants.EXTRA_MEMO_ID, memoId)
        }
        val openPending = PendingIntent.getActivity(
            context, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = Constants.ACTION_SNOOZE
            putExtra(Constants.EXTRA_MEMO_ID, memoId)
            putExtra(Constants.EXTRA_MEMO_TITLE, memoTitle)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context, notifId + 1, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = Constants.ACTION_DISMISS
            putExtra(Constants.EXTRA_MEMO_ID, memoId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context, notifId + 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VocalizeApplication.CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("Time to listen: $memoTitle")
            .setContentText("Tap to play your voice memo")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPending)
            .addAction(R.drawable.ic_play, "Play", openPending)
            .addAction(R.drawable.ic_alarm, "Snooze 10 min", snoozePending)
            .addAction(R.drawable.ic_close, "Dismiss", dismissPending)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        notificationManager.notify(notifId, notification)
    }

    fun buildPlaybackNotification(
        memoTitle: String,
        isPlaying: Boolean,
        pendingIntent: PendingIntent,
        playPauseIntent: PendingIntent,
        stopIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, VocalizeApplication.CHANNEL_PLAYBACK)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle(memoTitle)
            .setContentText(if (isPlaying) "Playing..." else "Paused")
            .setContentIntent(pendingIntent)
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pause" else "Play",
                playPauseIntent
            )
            .addAction(R.drawable.ic_close, "Stop", stopIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .build()
    }

    fun cancelNotification(memoId: String) {
        notificationManager.cancel(memoId.hashCode())
    }
}
