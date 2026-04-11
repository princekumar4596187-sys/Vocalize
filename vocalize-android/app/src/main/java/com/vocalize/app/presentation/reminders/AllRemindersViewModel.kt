package com.vocalize.app.presentation.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocalize.app.data.local.entity.ReminderEntity
import com.vocalize.app.data.local.entity.ReminderLogEntity
import com.vocalize.app.data.repository.MemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderItem(
    val reminder: ReminderEntity,
    val memoTitle: String,
    val log: ReminderLogEntity?
)

data class AllRemindersUiState(
    val upcoming: List<ReminderItem> = emptyList(),
    val past: List<ReminderItem> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AllRemindersViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AllRemindersUiState())
    val uiState: StateFlow<AllRemindersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                memoRepository.getAllRemindersFlow(),
                memoRepository.getAllMemos(),
                memoRepository.getAllReminderLogs()
            ) { reminders, memos, logs ->
                val memoMap = memos.associateBy { it.id }
                val logMap = logs.groupBy { it.reminderId }
                    .mapValues { entry -> entry.value.maxByOrNull { it.firedTime } }

                val now = System.currentTimeMillis()
                val items = reminders.map { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        memoTitle = memoMap[reminder.memoId]?.title?.ifBlank { "Voice Memo" } ?: "Voice Memo",
                        log = logMap[reminder.id]
                    )
                }

                AllRemindersUiState(
                    upcoming = items
                        .filter { it.reminder.reminderTime > now }
                        .sortedBy { it.reminder.reminderTime },
                    past = items
                        .filter { it.reminder.reminderTime <= now }
                        .sortedByDescending { it.reminder.reminderTime },
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }
}
