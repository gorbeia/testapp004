package com.example.testapp004.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val isAddDialogOpen: Boolean = false,
    val editingCategory: Category? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.categories.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun openAddDialog() {
        _uiState.update { it.copy(isAddDialogOpen = true) }
    }

    fun closeAddDialog() {
        _uiState.update { it.copy(isAddDialogOpen = false) }
    }

    fun addCategory(name: String, parentId: Long? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.addCategory(name.trim(), parentId)
            _uiState.update { it.copy(isAddDialogOpen = false) }
        }
    }

    fun openEditDialog(category: Category) {
        _uiState.update { it.copy(editingCategory = category) }
    }

    fun closeEditDialog() {
        _uiState.update { it.copy(editingCategory = null) }
    }

    fun updateCategory(categoryId: Long, name: String, parentId: Long?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            categoryRepository.updateCategory(categoryId, name.trim(), parentId)
            _uiState.update { it.copy(editingCategory = null) }
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(categoryId)
        }
    }
}
