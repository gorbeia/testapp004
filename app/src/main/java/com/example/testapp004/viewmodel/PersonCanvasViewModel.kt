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
    val relationDistance: Int = 0,
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

    private val relationDistanceFlow = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(
                acquaintanceRepository.acquaintances,
                relationRepository.relations,
                relationDistanceFlow,
            ) { acquaintances, relations, distance ->
                val centerPerson = acquaintances.find { it.id == acquaintanceId }
                    ?: return@combine PersonCanvasUiState(isLoading = false)

                val directRelatedIds = relations
                    .filter { it.fromId == acquaintanceId || it.toId == acquaintanceId }
                    .flatMap { listOf(it.fromId, it.toId) }
                    .filter { it != acquaintanceId }
                    .toSet()

                if (directRelatedIds.isEmpty()) {
                    return@combine PersonCanvasUiState(
                        personName = centerPerson.name,
                        nodes = emptyList(),
                        edges = emptyList(),
                        isLoading = false,
                        relationDistance = distance,
                    )
                }

                val reachedAfterDirect = setOf(acquaintanceId) + directRelatedIds
                val distanceOneIds: Set<Long> = if (distance >= 1) {
                    relations
                        .filter {
                            (it.fromId in directRelatedIds && it.toId !in reachedAfterDirect) ||
                                (it.toId in directRelatedIds && it.fromId !in reachedAfterDirect)
                        }
                        .flatMap { listOf(it.fromId, it.toId) }
                        .filter { it !in reachedAfterDirect }
                        .toSet()
                } else {
                    emptySet()
                }

                val reachedAfterOne = reachedAfterDirect + distanceOneIds
                val distanceTwoIds: Set<Long> = if (distance >= 2) {
                    relations
                        .filter {
                            (it.fromId in distanceOneIds && it.toId !in reachedAfterOne) ||
                                (it.toId in distanceOneIds && it.fromId !in reachedAfterOne)
                        }
                        .flatMap { listOf(it.fromId, it.toId) }
                        .filter { it !in reachedAfterOne }
                        .toSet()
                } else {
                    emptySet()
                }

                val distanceMap = buildMap<Long, Int> {
                    put(acquaintanceId, 0)
                    directRelatedIds.forEach { put(it, 0) }
                    distanceOneIds.forEach { put(it, 1) }
                    distanceTwoIds.forEach { put(it, 2) }
                }

                val visibleIds = distanceMap.keys
                val visibleRelations = relations.filter {
                    it.fromId in visibleIds && it.toId in visibleIds
                }

                val directPeople = acquaintances.filter { it.id in directRelatedIds }
                val d1People = acquaintances.filter { it.id in distanceOneIds }
                val d2People = acquaintances.filter { it.id in distanceTwoIds }

                val positions = mutableMapOf<Long, Pair<Float, Float>>()
                positions[acquaintanceId] = 0f to 0f

                val r1 = max(180f, directPeople.size * 60f)
                val n1 = directPeople.size
                directPeople.forEachIndexed { index, person ->
                    val angle = (2 * PI * index / n1.coerceAtLeast(1) - PI / 2).toFloat()
                    positions[person.id] = (r1 * cos(angle)) to (r1 * sin(angle))
                }

                if (d1People.isNotEmpty()) {
                    val r2 = r1 + max(150f, d1People.size * 50f)
                    val n2 = d1People.size
                    d1People.forEachIndexed { index, person ->
                        val angle = (2 * PI * index / n2.coerceAtLeast(1) - PI / 2).toFloat()
                        positions[person.id] = (r2 * cos(angle)) to (r2 * sin(angle))
                    }

                    if (d2People.isNotEmpty()) {
                        val r3 = r2 + max(130f, d2People.size * 40f)
                        val n3 = d2People.size
                        d2People.forEachIndexed { index, person ->
                            val angle = (2 * PI * index / n3.coerceAtLeast(1) - PI / 2).toFloat()
                            positions[person.id] = (r3 * cos(angle)) to (r3 * sin(angle))
                        }
                    }
                }

                val fromCounts = mutableMapOf<Long, Int>()
                val toCounts = mutableMapOf<Long, Int>()
                val categoryLists = mutableMapOf<Long, MutableList<RelationCategory>>()
                visibleRelations.forEach { rel ->
                    val relType = RelationTypes.findByKey(rel.typeKey)
                    val cat = relType?.category ?: return@forEach
                    categoryLists.getOrPut(rel.fromId) { mutableListOf() }.add(cat)
                    categoryLists.getOrPut(rel.toId) { mutableListOf() }.add(cat)
                    if (!relType.isSymmetric) {
                        fromCounts[rel.fromId] = (fromCounts[rel.fromId] ?: 0) + 1
                        toCounts[rel.toId] = (toCounts[rel.toId] ?: 0) + 1
                    }
                }

                val allVisiblePeople = acquaintances.filter { it.id in visibleIds }
                val nodes = allVisiblePeople.mapNotNull { person ->
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
                        distanceFromCategory = distanceMap[person.id] ?: 0,
                    )
                }

                val edges = visibleRelations.mapNotNull { rel ->
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
                    relationDistance = distance,
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

    fun setRelationDistance(d: Int) {
        relationDistanceFlow.value = d.coerceIn(0, 2)
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
