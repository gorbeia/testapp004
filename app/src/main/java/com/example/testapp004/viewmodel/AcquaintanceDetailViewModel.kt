package com.example.testapp004.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.data.RelationRepository
import com.example.testapp004.model.Acquaintance
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RelationDisplay(
    val relationId: Long,
    val label: String,
    val otherPersonId: Long,
    val otherPersonName: String,
    val isOutgoing: Boolean,
)

data class AcquaintanceDetailUiState(
    val acquaintance: Acquaintance? = null,
    val categoryName: String? = null,
    val relations: List<RelationDisplay> = emptyList(),
    val allOtherAcquaintances: List<Acquaintance> = emptyList(),
    val isAddRelationDialogOpen: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AcquaintanceDetailViewModel @Inject constructor(
    private val acquaintanceRepository: AcquaintanceRepository,
    private val categoryRepository: CategoryRepository,
    private val relationRepository: RelationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val acquaintanceId: Long = checkNotNull(savedStateHandle["acquaintanceId"])

    private val _uiState = MutableStateFlow(AcquaintanceDetailUiState())
    val uiState: StateFlow<AcquaintanceDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                acquaintanceRepository.acquaintances,
                categoryRepository.categories,
                relationRepository.relations,
            ) { acquaintances, categories, relations ->
                val acquaintance = acquaintances.find { it.id == acquaintanceId }
                val category = acquaintance?.categoryId?.let { catId -> categories.find { it.id == catId } }
                val relationsForPerson = relations.filter { it.fromId == acquaintanceId || it.toId == acquaintanceId }
                val relationDisplays = relationsForPerson.mapNotNull { relation ->
                    val isOutgoing = relation.fromId == acquaintanceId
                    val otherId = if (isOutgoing) relation.toId else relation.fromId
                    val other = acquaintances.find { it.id == otherId } ?: return@mapNotNull null
                    RelationDisplay(
                        relationId = relation.id,
                        label = relation.label,
                        otherPersonId = other.id,
                        otherPersonName = other.name,
                        isOutgoing = isOutgoing,
                    )
                }
                val others = acquaintances.filter { it.id != acquaintanceId }
                Triple(
                    AcquaintanceDetailUiState(
                        acquaintance = acquaintance,
                        categoryName = category?.name,
                        relations = relationDisplays,
                        allOtherAcquaintances = others,
                    ),
                    Unit,
                    Unit,
                )
            }.collect { (newState, _, _) ->
                _uiState.update { current ->
                    newState.copy(isAddRelationDialogOpen = current.isAddRelationDialogOpen)
                }
            }
        }
    }

    fun openAddRelationDialog() {
        _uiState.update { it.copy(isAddRelationDialogOpen = true) }
    }

    fun closeAddRelationDialog() {
        _uiState.update { it.copy(isAddRelationDialogOpen = false) }
    }

    fun addRelation(toId: Long, label: String) {
        if (label.isBlank()) return
        viewModelScope.launch {
            relationRepository.addRelation(fromId = acquaintanceId, toId = toId, label = label)
            _uiState.update { it.copy(isAddRelationDialogOpen = false) }
        }
    }

    fun deleteRelation(relationId: Long) {
        viewModelScope.launch {
            relationRepository.deleteRelation(relationId)
        }
    }

    fun deleteAcquaintance() {
        viewModelScope.launch {
            acquaintanceRepository.deleteAcquaintance(acquaintanceId)
        }
    }
}
