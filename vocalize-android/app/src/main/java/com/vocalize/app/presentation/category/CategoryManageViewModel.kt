package com.vocalize.app.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocalize.app.data.local.entity.CategoryEntity
import com.vocalize.app.data.repository.MemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryManageUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val message: String? = null
)

@HiltViewModel
class CategoryManageViewModel @Inject constructor(
    private val repository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManageUiState())
    val uiState: StateFlow<CategoryManageUiState> = _uiState.asStateFlow()

    init {
        observeCategories()
    }

    private fun observeCategories() {
        repository.getAllCategories()
            .onEach { categories ->
                _uiState.update {
                    it.copy(categories = categories, isLoading = false)
                }
            }
            .launchIn(viewModelScope)
    }

    fun addCategory(name: String, colorHex: String, iconName: String) {
        viewModelScope.launch {
            val category = CategoryEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                colorHex = colorHex,
                iconName = iconName
            )
            repository.insertCategory(category)
            _uiState.update { it.copy(message = "Category \"$name\" created") }
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
            _uiState.update { it.copy(message = "Category updated") }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _uiState.update { it.copy(message = "Category \"${category.name}\" deleted") }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
