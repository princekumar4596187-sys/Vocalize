package com.vocalize.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocalize.app.data.local.entity.CategoryEntity
import com.vocalize.app.data.local.entity.MemoEntity
import com.vocalize.app.data.local.entity.PlaylistEntity
import com.vocalize.app.data.local.entity.PlaylistMemoCrossRef
import com.vocalize.app.data.repository.MemoRepository
import com.vocalize.app.util.AudioFileManager
import com.vocalize.app.util.Constants
import com.vocalize.app.util.ReminderAlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val recentMemos: List<MemoEntity> = emptyList(),
    val allMemos: List<MemoEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val playlistMemoCounts: Map<String, Int> = emptyMap(),
    val selectedCategoryFilter: String? = null,
    val isLoading: Boolean = true,
    val totalMemos: Int = 0,
    val totalDurationMs: Long = 0L
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val memoRepository: MemoRepository,
    private val audioFileManager: AudioFileManager,
    private val alarmScheduler: ReminderAlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        seedDefaultCategories()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                memoRepository.getRecentMemos(),
                memoRepository.getAllMemos(),
                memoRepository.getAllPlaylists(),
                memoRepository.getAllCategories()
            ) { recent, all, playlists, categories ->
                HomeUiState(
                    recentMemos = recent,
                    allMemos = all,
                    playlists = playlists,
                    categories = categories,
                    isLoading = false,
                    totalMemos = all.size,
                    totalDurationMs = all.sumOf { it.duration }
                )
            }.collect { _uiState.value = it }
        }
    }

    private fun seedDefaultCategories() {
        viewModelScope.launch {
            val existing = memoRepository.getAllCategories().first()
            if (existing.isEmpty()) {
                Constants.DEFAULT_CATEGORIES.forEach { (id, name, color) ->
                    memoRepository.insertCategory(
                        CategoryEntity(
                            id = id,
                            name = name,
                            colorHex = color,
                            iconName = name.lowercase(),
                            isDefault = true
                        )
                    )
                }
            }
        }
    }

    fun deleteMemo(memo: MemoEntity) {
        viewModelScope.launch {
            if (memo.hasReminder) alarmScheduler.cancelReminder(memo.id)
            audioFileManager.deleteAudioFile(memo.filePath)
            memoRepository.deleteMemo(memo)
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            memoRepository.insertPlaylist(
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch { memoRepository.deletePlaylist(playlist) }
    }

    fun addMemoToPlaylist(memoId: String, playlistId: String) {
        viewModelScope.launch {
            memoRepository.addMemoToPlaylist(PlaylistMemoCrossRef(playlistId, memoId))
        }
    }

    fun setCategoryFilter(categoryId: String?) {
        _uiState.update { it.copy(selectedCategoryFilter = categoryId) }
    }

    fun updateMemoTitle(memoId: String, title: String) {
        viewModelScope.launch {
            memoRepository.updateTitle(memoId, title, System.currentTimeMillis())
        }
    }
}
