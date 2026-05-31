package com.example.testapp004.viewmodel

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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AcquaintancesUiState(
    val acquaintances: List<Acquaintance> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AcquaintancesViewModel @Inject constructor(
    private val acquaintanceRepository: AcquaintanceRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val categoryIdFilter = MutableStateFlow<Long?>(null)
    private val _uiState = MutableStateFlow(AcquaintancesUiState())
    val uiState: StateFlow<AcquaintancesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                acquaintanceRepository.acquaintances,
                categoryRepository.categories,
                categoryIdFilter,
            ) { acquaintances, categories, selectedCategoryId ->
                val filtered = if (selectedCategoryId == null) {
                    acquaintances
                } else {
                    acquaintances.filter { it.categoryId == selectedCategoryId }
                }
                AcquaintancesUiState(
                    acquaintances = filtered,
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectCategory(categoryId: Long?) {
        categoryIdFilter.value = categoryId
    }

    fun deleteAcquaintance(acquaintanceId: Long) {
        viewModelScope.launch {
            acquaintanceRepository.deleteAcquaintance(acquaintanceId)
        }
    }
}
