package com.vocalize.app.widget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocalize.app.data.local.entity.CategoryEntity
import com.vocalize.app.data.local.entity.MemoEntity
import com.vocalize.app.data.local.entity.ReminderEntity
import com.vocalize.app.data.local.entity.RepeatType
import com.vocalize.app.data.repository.MemoRepository
import com.vocalize.app.util.AudioRecorderManager
import com.vocalize.app.util.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

enum class WidgetRecorderPhase { READY, RECORDING, STOPPED, SAVING, SAVED, ERROR }

enum class ReminderPreset(val label: String) {
    NONE("No reminder"),
    IN_30_MIN("In 30 minutes"),
    IN_1_HOUR("In 1 hour"),
    IN_2_HOURS("In 2 hours"),
    TOMORROW_9AM("Tomorrow at 9:00 AM")
}

data class WidgetRecorderUiState(
    val phase: WidgetRecorderPhase = WidgetRecorderPhase.READY,
    val elapsedMs: Long = 0L,
    val filePath: String? = null,
    val duration: Long = 0L,
    val title: String = "",
    val note: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val reminderPreset: ReminderPreset = ReminderPreset.NONE,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class WidgetRecorderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioRecorderManager: AudioRecorderManager,
    private val memoRepository: MemoRepository,
    private val alarmScheduler: ReminderAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(WidgetRecorderUiState())
    val uiState: StateFlow<WidgetRecorderUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            memoRepository.getAllCategories().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun startRecording() {
        if (audioRecorderManager.isRecording) return
        try {
            val path = audioRecorderManager.startRecording()
            _uiState.update { it.copy(phase = WidgetRecorderPhase.RECORDING, filePath = path, elapsedMs = 0L) }
            timerJob = viewModelScope.launch {
                val start = System.currentTimeMillis()
                while (true) {
                    delay(100)
                    _uiState.update { it.copy(elapsedMs = System.currentTimeMillis() - start) }
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(phase = WidgetRecorderPhase.ERROR, errorMessage = e.message) }
        }
    }

    fun stopRecording() {
        timerJob?.cancel()
        val result = audioRecorderManager.stopRecording()
        _uiState.update {
            it.copy(
                phase = WidgetRecorderPhase.STOPPED,
                duration = result?.second ?: it.elapsedMs,
                filePath = result?.first ?: it.filePath
            )
        }
    }

    fun cancelRecording() {
        timerJob?.cancel()
        audioRecorderManager.cancelRecording()
    }

    fun setTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun setNote(value: String) = _uiState.update { it.copy(note = value) }
    fun setCategory(id: String?) = _uiState.update { it.copy(selectedCategoryId = id) }
    fun setReminderPreset(preset: ReminderPreset) = _uiState.update { it.copy(reminderPreset = preset) }

    fun saveMemo() {
        val state = _uiState.value
        val path = state.filePath ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val memoId = UUID.randomUUID().toString()
                val reminderTime = resolveReminderTime(state.reminderPreset)
                val displayTitle = state.title.trim().ifBlank { "Voice Memo" }

                val memo = MemoEntity(
                    id = memoId,
                    title = displayTitle,
                    filePath = path,
                    duration = state.duration,
                    dateCreated = System.currentTimeMillis(),
                    dateModified = System.currentTimeMillis(),
                    categoryId = state.selectedCategoryId,
                    textNote = state.note.trim(),
                    hasReminder = reminderTime != null,
                    reminderTime = reminderTime
                )
                memoRepository.insertMemo(memo)

                if (reminderTime != null) {
                    val reminder = ReminderEntity(
                        id = UUID.randomUUID().toString(),
                        memoId = memoId,
                        reminderTime = reminderTime,
                        repeatType = RepeatType.NONE,
                        customDays = ""
                    )
                    memoRepository.insertReminder(reminder)
                    alarmScheduler.scheduleReminder(reminder, displayTitle)
                }

                VocalizeWidget.requestWidgetRefresh(context)
                _uiState.update { it.copy(phase = WidgetRecorderPhase.SAVED, isSaving = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    private fun resolveReminderTime(preset: ReminderPreset): Long? {
        val now = System.currentTimeMillis()
        return when (preset) {
            ReminderPreset.NONE -> null
            ReminderPreset.IN_30_MIN -> now + 30 * 60_000L
            ReminderPreset.IN_1_HOUR -> now + 60 * 60_000L
            ReminderPreset.IN_2_HOURS -> now + 2 * 60 * 60_000L
            ReminderPreset.TOMORROW_9AM -> Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}
