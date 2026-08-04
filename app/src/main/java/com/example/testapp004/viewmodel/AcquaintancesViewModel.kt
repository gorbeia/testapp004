package com.example.testapp004.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.model.Acquaintance
import com.example.testapp004.model.Category
import com.example.testapp004.model.descendantsAndSelf
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
    val searchQuery: String = "",
    val expandedCategoryIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AcquaintancesViewModel @Inject constructor(
    private val acquaintanceRepository: AcquaintanceRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {
    private val categoryIdFilter = MutableStateFlow<Long?>(null)
    private val searchQueryFlow = MutableStateFlow("")
    private val expandedCategoryIdsFlow = MutableStateFlow<Set<Long>>(emptySet())
    private val _uiState = MutableStateFlow(AcquaintancesUiState())
    val uiState: StateFlow<AcquaintancesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                acquaintanceRepository.acquaintances,
                categoryRepository.categories,
                categoryIdFilter,
                searchQueryFlow,
                expandedCategoryIdsFlow,
            ) { acquaintances, categories, selectedCategoryId, searchQuery, expandedCategoryIds ->
                val categoryFiltered = if (selectedCategoryId == null) {
                    acquaintances
                } else {
                    val includedIds = categories.descendantsAndSelf(selectedCategoryId)
                    acquaintances.filter { person -> person.categoryIds.any { it in includedIds } }
                }
                val filtered = if (searchQuery.isBlank()) {
                    categoryFiltered
                } else {
                    val q = searchQuery.trim()
                    categoryFiltered.filter { person ->
                        person.name.contains(q, ignoreCase = true) ||
                            person.bio.contains(q, ignoreCase = true)
                    }
                }
                AcquaintancesUiState(
                    acquaintances = filtered,
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    searchQuery = searchQuery,
                    expandedCategoryIds = expandedCategoryIds,
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectCategory(categoryId: Long?) {
        categoryIdFilter.value = categoryId
    }

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun toggleCategoryExpanded(id: Long) {
        val current = expandedCategoryIdsFlow.value
        expandedCategoryIdsFlow.value = if (id in current) current - id else current + id
    }
}
