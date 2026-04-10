package com.vocalize.app.util

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPosition: Int = 0,
    val duration: Int = 0,
    val currentMemoId: String? = null,
    val playbackSpeed: Float = 1.0f
)

@Singleton
class AudioPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaPlayer: MediaPlayer? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun prepareAndPlay(filePath: String, memoId: String) {
        release()
        val file = File(filePath)
        if (!file.exists()) return

        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            prepare()
            start()
            setOnCompletionListener {
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPosition = 0,
                    currentMemoId = null
                )
            }
            _playbackState.value = PlaybackState(
                isPlaying = true,
                currentPosition = 0,
                duration = this.duration,
                currentMemoId = memoId,
                playbackSpeed = _playbackState.value.playbackSpeed
            )
        }
        applySpeed(_playbackState.value.playbackSpeed)
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _playbackState.value = _playbackState.value.copy(isPlaying = false)
            } else {
                player.start()
                _playbackState.value = _playbackState.value.copy(isPlaying = true)
            }
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPosition = positionMs)
    }

    fun setSpeed(speed: Float) {
        _playbackState.value = _playbackState.value.copy(playbackSpeed = speed)
        applySpeed(speed)
    }

    private fun applySpeed(speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let { player ->
                val params = player.playbackParams.setSpeed(speed)
                player.playbackParams = params
            }
        }
    }

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    fun release() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
        _playbackState.value = PlaybackState()
    }

    fun updatePosition() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                _playbackState.value = _playbackState.value.copy(
                    currentPosition = player.currentPosition
                )
            }
        }
    }
}
