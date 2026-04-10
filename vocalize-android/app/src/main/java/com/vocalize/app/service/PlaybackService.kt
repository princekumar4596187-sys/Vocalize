package com.vocalize.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.vocalize.app.MainActivity
import com.vocalize.app.R
import com.vocalize.app.VocalizeApplication
import com.vocalize.app.util.AudioPlayerManager
import com.vocalize.app.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class PlaybackService : Service() {

    @Inject lateinit var audioPlayerManager: AudioPlayerManager
    @Inject lateinit var notificationHelper: NotificationHelper

    private val binder = LocalBinder()
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var positionUpdateJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): PlaybackService = this@PlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification(false))
        startPositionUpdates()
    }

    private fun startPositionUpdates() {
        positionUpdateJob = scope.launch {
            while (isActive) {
                audioPlayerManager.updatePosition()
                delay(500)
            }
        }
    }

    fun playMemo(filePath: String, memoId: String, memoTitle: String) {
        audioPlayerManager.prepareAndPlay(filePath, memoId)
        updateNotification(memoTitle, true)
    }

    fun togglePlayPause(memoTitle: String) {
        audioPlayerManager.togglePlayPause()
        updateNotification(memoTitle, audioPlayerManager.isPlaying())
    }

    fun stop() {
        audioPlayerManager.release()
        stopSelf()
    }

    private fun buildNotification(isPlaying: Boolean, title: String = "Vocalize"): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, PlaybackService::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, PlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return notificationHelper.buildPlaybackNotification(title, isPlaying, openIntent, playPauseIntent, stopIntent)
    }

    private fun updateNotification(title: String, isPlaying: Boolean) {
        val notification = buildNotification(isPlaying, title)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> togglePlayPause("Voice Memo")
            ACTION_STOP -> stop()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        positionUpdateJob?.cancel()
        scope.cancel()
        audioPlayerManager.release()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_TOGGLE = "com.vocalize.app.TOGGLE"
        const val ACTION_STOP = "com.vocalize.app.STOP"
    }
}
