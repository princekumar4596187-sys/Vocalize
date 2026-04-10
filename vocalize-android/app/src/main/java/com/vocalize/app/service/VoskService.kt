package com.vocalize.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.vocalize.app.data.repository.MemoRepository
import com.vocalize.app.util.VoskTranscriber
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class VoskService : Service() {

    @Inject lateinit var voskTranscriber: VoskTranscriber
    @Inject lateinit var memoRepository: MemoRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val memoId = intent?.getStringExtra(EXTRA_MEMO_ID) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH) ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch {
            val transcription = voskTranscriber.transcribeFile(filePath)
            if (transcription.isNotBlank()) {
                memoRepository.updateTranscription(memoId, transcription)
            }
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        const val EXTRA_MEMO_ID = "extra_memo_id"
        const val EXTRA_FILE_PATH = "extra_file_path"
    }
}
