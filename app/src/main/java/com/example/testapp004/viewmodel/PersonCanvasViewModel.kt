package com.example.testapp004.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.RelationRepository
import com.example.testapp004.model.Relation
import com.example.testapp004.model.RelationTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    relationRepository: RelationRepository,
) : CanvasViewModel(relationRepository) {
    val acquaintanceId: Long = checkNotNull(savedStateHandle["acquaintanceId"])

    private val _uiState = MutableStateFlow(PersonCanvasUiState())
    val uiState: StateFlow<PersonCanvasUiState> = _uiState.asStateFlow()

    private val relationDistanceFlow = MutableStateFlow(0)

    override val dialogFromId get() = _uiState.value.pendingRelationFromId
    override val dialogToId get() = _uiState.value.pendingRelationToId
    override fun nodesSnapshot() = _uiState.value.nodes
    override fun applyDialogState(
        isOpen: Boolean, fromId: Long?, toId: Long?, fromName: String, toName: String,
    ) {
        _uiState.update {
            it.copy(
                isRelationDialogOpen = isOpen,
                pendingRelationFromId = fromId,
                pendingRelationToId = toId,
                pendingRelationFromName = fromName,
                pendingRelationToName = toName,
            )
        }
    }

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

                val positions = computeHierarchicalPositions(acquaintanceId, visibleIds, visibleRelations)

                val nodes = buildCanvasNodes(
                    acquaintances = acquaintances,
                    visibleIds = visibleIds,
                    positions = positions,
                    distanceMap = distanceMap,
                    visibleRelations = visibleRelations,
                    isDirectMember = { id -> id == acquaintanceId },
                )
                val edges = buildCanvasEdges(visibleRelations)

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

    private fun computeHierarchicalPositions(
        centerId: Long,
        visibleIds: Set<Long>,
        visibleRelations: List<Relation>,
    ): Map<Long, Pair<Float, Float>> {
        val levelMap = mutableMapOf(centerId to 0)
        val queue = ArrayDeque<Long>()
        queue.add(centerId)
        while (queue.isNotEmpty()) {
            val nodeId = queue.removeFirst()
            val nodeLevel = levelMap[nodeId] ?: continue
            for (rel in visibleRelations) {
                val delta = RelationTypes.findByKey(rel.typeKey)?.verticalDelta ?: 0
                when {
                    rel.fromId == nodeId && rel.toId !in levelMap -> {
                        levelMap[rel.toId] = nodeLevel - delta
                        queue.add(rel.toId)
                    }
                    rel.toId == nodeId && rel.fromId !in levelMap -> {
                        levelMap[rel.fromId] = nodeLevel + delta
                        queue.add(rel.fromId)
                    }
                }
            }
        }
        visibleIds.forEach { id -> levelMap.getOrPut(id) { 0 } }

        val levelGroups = levelMap.entries.groupBy({ it.value }, { it.key })
        val allLevels = levelGroups.keys.sorted()
        val layerHeight = 170f
        val nodeSpacing = 220f
        val positions = mutableMapOf<Long, Pair<Float, Float>>()

        for ((level, ids) in levelGroups) {
            val n = ids.size
            ids.forEachIndexed { i, id ->
                positions[id] = (-(n - 1) / 2f + i) * nodeSpacing to -level * layerHeight
            }
        }

        repeat(4) { pass ->
            val levelOrder = if (pass % 2 == 0) allLevels else allLevels.reversed()
            for (level in levelOrder) {
                val ids = levelGroups[level] ?: continue
                if (ids.size <= 1) continue
                val withScore = ids.map { id ->
                    val xs = visibleRelations.mapNotNull { rel ->
                        val neighbor = when {
                            rel.fromId == id -> rel.toId
                            rel.toId == id -> rel.fromId
                            else -> null
                        }
                        neighbor?.takeIf { levelMap[it] != level }?.let { positions[it]?.first }
                    }
                    id to if (xs.isEmpty()) positions[id]?.first ?: 0f else xs.average().toFloat()
                }.sortedBy { it.second }
                val n = withScore.size
                withScore.forEachIndexed { i, (id, _) ->
                    val y = positions[id]?.second ?: (-level * layerHeight)
                    positions[id] = (-(n - 1) / 2f + i) * nodeSpacing to y
                }
            }
        }

        val cx = positions[centerId]?.first ?: 0f
        val cy = positions[centerId]?.second ?: 0f
        return positions.mapValues { (_, pos) -> (pos.first - cx) to (pos.second - cy) }
    }
}
