package com.example.testapp004.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.model.Acquaintance
import com.example.testapp004.model.Category
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditAcquaintanceUiState(
    val name: String = "",
    val bio: String = "",
    val selectedCategoryIds: Set<Long> = emptySet(),
    val categories: List<Category> = emptyList(),
    val isEditing: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AddEditAcquaintanceViewModel @Inject constructor(
    private val acquaintanceRepository: AcquaintanceRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val acquaintanceId: Long? =
        savedStateHandle.get<Long>("acquaintanceId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(AddEditAcquaintanceUiState(isEditing = acquaintanceId != null))
    val uiState: StateFlow<AddEditAcquaintanceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.categories.collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
        if (acquaintanceId != null) {
            viewModelScope.launch {
                val acquaintance = acquaintanceRepository.getAcquaintance(acquaintanceId)
                if (acquaintance != null) {
                    _uiState.update {
                        it.copy(
                            name = acquaintance.name,
                            bio = acquaintance.bio,
                            selectedCategoryIds = acquaintance.categoryIds,
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onBioChange(bio: String) {
        _uiState.update { it.copy(bio = bio) }
    }

    fun onCategoryToggled(categoryId: Long) {
        _uiState.update { state ->
            val newIds = if (categoryId in state.selectedCategoryIds) {
                state.selectedCategoryIds - categoryId
            } else {
                state.selectedCategoryIds + categoryId
            }
            state.copy(selectedCategoryIds = newIds)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            if (acquaintanceId == null) {
                acquaintanceRepository.addAcquaintance(
                    name = state.name.trim(),
                    bio = state.bio.trim(),
                    categoryIds = state.selectedCategoryIds,
                )
            } else {
                acquaintanceRepository.updateAcquaintance(
                    Acquaintance(
                        id = acquaintanceId,
                        name = state.name.trim(),
                        bio = state.bio.trim(),
                        categoryIds = state.selectedCategoryIds,
                    ),
                )
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
