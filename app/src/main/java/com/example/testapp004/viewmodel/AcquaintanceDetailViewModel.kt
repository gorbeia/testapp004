package com.example.testapp004.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.data.ContactRepository
import com.example.testapp004.data.RelationRepository
import com.example.testapp004.model.Acquaintance
import com.example.testapp004.model.Category
import com.example.testapp004.model.ContactInfo
import com.example.testapp004.model.RelationTypes
import com.example.testapp004.model.labelFor
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
    val counterpartLabel: String,
    val otherPersonId: Long,
    val otherPersonName: String,
    val isOutgoing: Boolean,
)

data class AcquaintanceDetailUiState(
    val acquaintance: Acquaintance? = null,
    val categoryNames: List<String> = emptyList(),
    val relations: List<RelationDisplay> = emptyList(),
    val allOtherAcquaintances: List<Acquaintance> = emptyList(),
    val allCategories: List<Category> = emptyList(),
    val linkedContactInfo: ContactInfo? = null,
    val isAddRelationDialogOpen: Boolean = false,
    val pendingNewRelationPersonId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AcquaintanceDetailViewModel @Inject constructor(
    private val acquaintanceRepository: AcquaintanceRepository,
    private val categoryRepository: CategoryRepository,
    private val relationRepository: RelationRepository,
    private val contactRepository: ContactRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val acquaintanceId: Long = checkNotNull(savedStateHandle["acquaintanceId"])

    private val _uiState = MutableStateFlow(AcquaintanceDetailUiState())
    val uiState: StateFlow<AcquaintanceDetailUiState> = _uiState.asStateFlow()

    private var lastLookupKey: String? = null

    init {
        viewModelScope.launch {
            combine(
                acquaintanceRepository.acquaintances,
                categoryRepository.categories,
                relationRepository.relations,
            ) { acquaintances, categories, relations ->
                val acquaintance = acquaintances.find { it.id == acquaintanceId }
                val categoryNames = acquaintance?.categoryIds
                    ?.mapNotNull { catId -> categories.find { it.id == catId }?.name }
                    ?: emptyList()
                val relationsForPerson = relations.filter { it.fromId == acquaintanceId || it.toId == acquaintanceId }
                val relationDisplays = relationsForPerson.mapNotNull { relation ->
                    val isOutgoing = relation.fromId == acquaintanceId
                    val otherId = if (isOutgoing) relation.toId else relation.fromId
                    val other = acquaintances.find { it.id == otherId } ?: return@mapNotNull null
                    val counterpartLabel = if (relation.typeKey == RelationTypes.CUSTOM_KEY) {
                        relation.customLabel.orEmpty()
                    } else {
                        RelationTypes.findByKey(relation.typeKey)?.let { type ->
                            if (isOutgoing) type.toLabel else type.fromLabel
                        } ?: relation.customLabel.orEmpty()
                    }
                    RelationDisplay(
                        relationId = relation.id,
                        label = relation.labelFor(acquaintanceId),
                        counterpartLabel = counterpartLabel,
                        otherPersonId = other.id,
                        otherPersonName = other.name,
                        isOutgoing = isOutgoing,
                    )
                }
                AcquaintanceDetailUiState(
                    acquaintance = acquaintance,
                    categoryNames = categoryNames,
                    relations = relationDisplays,
                    allOtherAcquaintances = acquaintances.filter { it.id != acquaintanceId },
                    allCategories = categories,
                )
            }.collect { baseState ->
                _uiState.update { current ->
                    baseState.copy(
                        isAddRelationDialogOpen = current.isAddRelationDialogOpen,
                        linkedContactInfo = current.linkedContactInfo,
                    )
                }

                val newKey = baseState.acquaintance?.androidContactLookupKey
                if (newKey != lastLookupKey) {
                    lastLookupKey = newKey
                    launch {
                        val contactInfo = newKey?.let { contactRepository.lookupContactByKey(it) }
                        _uiState.update { it.copy(linkedContactInfo = contactInfo) }
                    }
                }
            }
        }
    }

    fun openAddRelationDialog() {
        _uiState.update { it.copy(isAddRelationDialogOpen = true, pendingNewRelationPersonId = null) }
    }

    fun openAddRelationDialogWithPreselectedPerson(personId: Long) {
        _uiState.update { it.copy(isAddRelationDialogOpen = true, pendingNewRelationPersonId = personId) }
    }

    fun closeAddRelationDialog() {
        _uiState.update { it.copy(isAddRelationDialogOpen = false, pendingNewRelationPersonId = null) }
    }

    fun addRelation(otherId: Long, typeKey: String, isCurrentPersonFrom: Boolean, customLabel: String?) {
        if (typeKey == RelationTypes.CUSTOM_KEY && customLabel.isNullOrBlank()) return
        val fromId = if (isCurrentPersonFrom) acquaintanceId else otherId
        val toId = if (isCurrentPersonFrom) otherId else acquaintanceId
        viewModelScope.launch {
            relationRepository.addRelation(fromId = fromId, toId = toId, typeKey = typeKey, customLabel = customLabel)
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

    fun linkContact(contactUriString: String) {
        viewModelScope.launch {
            val info = contactRepository.lookupContactByUri(Uri.parse(contactUriString))
            if (info != null) {
                val updated = _uiState.value.acquaintance?.copy(androidContactLookupKey = info.lookupKey)
                    ?: return@launch
                acquaintanceRepository.updateAcquaintance(updated)
            }
        }
    }

    fun unlinkContact() {
        viewModelScope.launch {
            val updated = _uiState.value.acquaintance?.copy(androidContactLookupKey = null)
                ?: return@launch
            acquaintanceRepository.updateAcquaintance(updated)
        }
    }

    fun saveBio(bio: String) {
        val acquaintance = _uiState.value.acquaintance ?: return
        viewModelScope.launch {
            acquaintanceRepository.updateAcquaintance(acquaintance.copy(bio = bio.trim()))
        }
    }

    fun toggleDeceased() {
        val acquaintance = _uiState.value.acquaintance ?: return
        viewModelScope.launch {
            acquaintanceRepository.updateAcquaintance(acquaintance.copy(isDeceased = !acquaintance.isDeceased))
        }
    }
}
