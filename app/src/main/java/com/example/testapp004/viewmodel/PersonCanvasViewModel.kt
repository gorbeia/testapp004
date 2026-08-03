package com.example.testapp004.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.RelationRepository
import com.example.testapp004.model.RelationCategory
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

data class PersonCanvasUiState(
    val personName: String = "",
    val nodes: List<CanvasPersonNode> = emptyList(),
    val edges: List<CanvasRelationEdge> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isRelationDialogOpen: Boolean = false,
    val pendingRelationFromId: Long? = null,
    val pendingRelationToId: Long? = null,
    val pendingRelationFromName: String = "",
    val pendingRelationToName: String = "",
)

@HiltViewModel
class PersonCanvasViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val acquaintanceRepository: AcquaintanceRepository,
    private val relationRepository: RelationRepository,
) : ViewModel() {
    val acquaintanceId: Long = checkNotNull(savedStateHandle["acquaintanceId"])

    private val _uiState = MutableStateFlow(PersonCanvasUiState())
    val uiState: StateFlow<PersonCanvasUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                acquaintanceRepository.acquaintances,
                relationRepository.relations,
            ) { acquaintances, relations ->
                val personRelations = relations.filter {
                    it.fromId == acquaintanceId || it.toId == acquaintanceId
                }

                val centerPerson = acquaintances.find { it.id == acquaintanceId }
                    ?: return@combine PersonCanvasUiState(isLoading = false)

                if (personRelations.isEmpty()) {
                    return@combine PersonCanvasUiState(
                        personName = centerPerson.name,
                        nodes = emptyList(),
                        edges = emptyList(),
                        isLoading = false,
                    )
                }

                val relatedIds = personRelations
                    .flatMap { listOf(it.fromId, it.toId) }
                    .filter { it != acquaintanceId }
                    .toSet()
                val relatedPeople = acquaintances.filter { it.id in relatedIds }

                val ringRadius = max(180f, relatedPeople.size * 60f)
                val positions = mutableMapOf<Long, Pair<Float, Float>>()
                positions[acquaintanceId] = 0f to 0f
                val n = relatedPeople.size
                relatedPeople.forEachIndexed { index, person ->
                    val angle = (2 * PI * index / n.coerceAtLeast(1) - PI / 2).toFloat()
                    positions[person.id] = (ringRadius * cos(angle)) to (ringRadius * sin(angle))
                }

                val fromCounts = mutableMapOf<Long, Int>()
                val toCounts = mutableMapOf<Long, Int>()
                val categoryLists = mutableMapOf<Long, MutableList<RelationCategory>>()
                personRelations.forEach { rel ->
                    val relType = RelationTypes.findByKey(rel.typeKey)
                    val cat = relType?.category ?: return@forEach
                    categoryLists.getOrPut(rel.fromId) { mutableListOf() }.add(cat)
                    categoryLists.getOrPut(rel.toId) { mutableListOf() }.add(cat)
                    if (!relType.isSymmetric) {
                        fromCounts[rel.fromId] = (fromCounts[rel.fromId] ?: 0) + 1
                        toCounts[rel.toId] = (toCounts[rel.toId] ?: 0) + 1
                    }
                }

                val allPeople = listOf(centerPerson) + relatedPeople
                val nodes = allPeople.mapNotNull { person ->
                    val (x, y) = positions[person.id] ?: return@mapNotNull null
                    val dominant = categoryLists[person.id]
                        ?.groupingBy { it }
                        ?.eachCount()
                        ?.maxByOrNull { it.value }
                        ?.key
                    val outDegree = fromCounts[person.id] ?: 0
                    val inDegree = toCounts[person.id] ?: 0
                    val isNetSource = when {
                        outDegree > inDegree -> true
                        inDegree > outDegree -> false
                        else -> null
                    }
                    CanvasPersonNode(
                        id = person.id,
                        name = person.name,
                        x = x,
                        y = y,
                        dominantCategory = dominant,
                        isDirectMember = person.id == acquaintanceId,
                        isNetSource = isNetSource,
                        distanceFromCategory = 0,
                    )
                }

                val edges = personRelations.mapNotNull { rel ->
                    val relType = RelationTypes.findByKey(rel.typeKey)
                    CanvasRelationEdge(
                        id = rel.id,
                        fromId = rel.fromId,
                        toId = rel.toId,
                        label = rel.labelFor(rel.fromId),
                        category = relType?.category,
                        isSymmetric = relType?.isSymmetric ?: false,
                    )
                }

                PersonCanvasUiState(
                    personName = centerPerson.name,
                    nodes = nodes,
                    edges = edges,
                    isLoading = false,
                )
            }.collect { newState ->
                _uiState.update { current ->
                    newState.copy(
                        isRelationDialogOpen = current.isRelationDialogOpen,
                        pendingRelationFromId = current.pendingRelationFromId,
                        pendingRelationToId = current.pendingRelationToId,
                        pendingRelationFromName = current.pendingRelationFromName,
                        pendingRelationToName = current.pendingRelationToName,
                    )
                }
            }
        }
    }

    fun openRelationDialog(fromId: Long, toId: Long) {
        val nodes = _uiState.value.nodes
        val fromName = nodes.find { it.id == fromId }?.name ?: ""
        val toName = nodes.find { it.id == toId }?.name ?: ""
        _uiState.update {
            it.copy(
                isRelationDialogOpen = true,
                pendingRelationFromId = fromId,
                pendingRelationToId = toId,
                pendingRelationFromName = fromName,
                pendingRelationToName = toName,
            )
        }
    }

    fun closeRelationDialog() {
        _uiState.update {
            it.copy(
                isRelationDialogOpen = false,
                pendingRelationFromId = null,
                pendingRelationToId = null,
                pendingRelationFromName = "",
                pendingRelationToName = "",
            )
        }
    }

    fun addRelationFromCanvas(typeKey: String, isDragSourceFrom: Boolean, customLabel: String?) {
        val state = _uiState.value
        val dragFromId = state.pendingRelationFromId ?: return
        val dragToId = state.pendingRelationToId ?: return
        if (typeKey == RelationTypes.CUSTOM_KEY && customLabel.isNullOrBlank()) return
        val actualFromId = if (isDragSourceFrom) dragFromId else dragToId
        val actualToId = if (isDragSourceFrom) dragToId else dragFromId
        viewModelScope.launch {
            relationRepository.addRelation(
                fromId = actualFromId,
                toId = actualToId,
                typeKey = typeKey,
                customLabel = customLabel,
            )
            closeRelationDialog()
        }
    }
}
