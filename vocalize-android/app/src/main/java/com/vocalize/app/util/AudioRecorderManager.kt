package com.vocalize.app.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFilePath: String? = null
    private var startTime: Long = 0L
    var isRecording = false
        private set

    fun startRecording(): String {
        val recordingsDir = File(context.filesDir, Constants.RECORDINGS_DIR).apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val uniqueId = UUID.randomUUID().toString().take(8)
        val fileName = "${timestamp}_${uniqueId}.m4a"
        val filePath = File(recordingsDir, fileName).absolutePath
        currentFilePath = filePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44100)
            setOutputFile(filePath)
            prepare()
            start()
        }

        startTime = System.currentTimeMillis()
        isRecording = true
        return filePath
    }

    fun stopRecording(): Pair<String, Long>? {
        if (!isRecording) return null
        val duration = System.currentTimeMillis() - startTime
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        isRecording = false
        return currentFilePath?.let { it to duration }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        mediaRecorder = null
        isRecording = false
        currentFilePath?.let { File(it).delete() }
        currentFilePath = null
    }

    fun getMaxAmplitude(): Int {
        return if (isRecording) {
            mediaRecorder?.maxAmplitude ?: 0
        } else 0
    }
}
