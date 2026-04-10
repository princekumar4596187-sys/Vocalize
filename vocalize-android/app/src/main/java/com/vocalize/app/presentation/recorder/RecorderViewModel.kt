package com.vocalize.app.presentation.recorder

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocalize.app.data.local.entity.MemoEntity
import com.vocalize.app.data.local.entity.RepeatType
import com.vocalize.app.data.repository.MemoRepository
import com.vocalize.app.service.VoskService
import com.vocalize.app.util.AudioPlayerManager
import com.vocalize.app.util.AudioRecorderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import javax.inject.Inject

data class RecorderUiState(
    val isRecording: Boolean = false,
    val isStopped: Boolean = false,
    val isSaving: Boolean = false,
    val isPreviewPlaying: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitudeHistory: List<Float> = emptyList(),
    val title: String = "",
    val textNote: String = "",
    val transcription: String = "",
    val setReminderAfterSave: Boolean = false,
    val savedMemoId: String? = null,
    val filePath: String? = null,
    val duration: Long = 0L
)

@HiltViewModel
class RecorderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorderManager: AudioRecorderManager,
    private val audioPlayerManager: AudioPlayerManager,
    private val memoRepository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecorderUiState())
    val uiState: StateFlow<RecorderUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null
    private var startTime = 0L

    fun startRecording() {
        val filePath = audioRecorderManager.startRecording()
        startTime = System.currentTimeMillis()
        _uiState.update { it.copy(isRecording = true, isStopped = false, filePath = filePath) }
        startTimer()
        startAmplitudeSampling()
    }

    fun stopRecording() {
        val result = audioRecorderManager.stopRecording()
        timerJob?.cancel()
        amplitudeJob?.cancel()
        _uiState.update {
            it.copy(
                isRecording = false,
                isStopped = true,
                duration = result?.second ?: 0L,
                filePath = result?.first ?: it.filePath
            )
        }
    }

    fun cancelRecording() {
        timerJob?.cancel()
        amplitudeJob?.cancel()
        audioRecorderManager.cancelRecording()
        audioPlayerManager.release()
        _uiState.value = RecorderUiState()
    }

    fun togglePreviewPlayback() {
        val state = _uiState.value
        if (state.filePath == null) return
        if (state.isPreviewPlaying) {
            audioPlayerManager.togglePlayPause()
            _uiState.update { it.copy(isPreviewPlaying = false) }
        } else {
            audioPlayerManager.prepareAndPlay(state.filePath, "preview")
            _uiState.update { it.copy(isPreviewPlaying = true) }
        }
    }

    fun updateTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun updateNote(note: String) = _uiState.update { it.copy(textNote = note) }
    fun toggleSetReminder(enabled: Boolean) = _uiState.update { it.copy(setReminderAfterSave = enabled) }

    fun save(onSaved: ((String) -> Unit)? = null) {
        val state = _uiState.value
        val filePath = state.filePath ?: return
        _uiState.update { it.copy(isSaving = true) }
        audioPlayerManager.release()

        viewModelScope.launch {
            val memoId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val title = state.title.ifBlank { "Voice Memo" }
            val memo = MemoEntity(
                id = memoId,
                title = title,
                filePath = filePath,
                duration = state.duration,
                dateCreated = now,
                dateModified = now,
                textNote = state.textNote,
                repeatType = RepeatType.NONE,
                isTranscribing = true
            )
            memoRepository.insertMemo(memo)

            // Launch Vosk transcription in background
            val voskIntent = Intent(context, VoskService::class.java).apply {
                putExtra(VoskService.EXTRA_MEMO_ID, memoId)
                putExtra(VoskService.EXTRA_FILE_PATH, filePath)
            }
            context.startService(voskIntent)

            _uiState.update { it.copy(isSaving = false, savedMemoId = memoId) }
            onSaved?.invoke(memoId)
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(elapsedMs = System.currentTimeMillis() - startTime) }
                delay(100)
            }
        }
    }

    private fun startAmplitudeSampling() {
        amplitudeJob = viewModelScope.launch {
            while (isActive) {
                val amp = audioRecorderManager.getMaxAmplitude()
                val normalized = (amp / 32767f).coerceIn(0f, 1f)
                _uiState.update { state ->
                    val updated = (state.amplitudeHistory + normalized).takeLast(50)
                    state.copy(amplitudeHistory = updated)
                }
                delay(80)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        amplitudeJob?.cancel()
        audioPlayerManager.release()
    }
}
