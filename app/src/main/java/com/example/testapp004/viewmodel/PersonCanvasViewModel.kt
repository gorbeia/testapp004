package com.example.testapp004.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.RelationRepository
import com.example.testapp004.model.Relation
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

                val positions = computeHierarchicalPositions(acquaintanceId, visibleIds, visibleRelations)

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

    private fun computeHierarchicalPositions(
        centerId: Long,
        visibleIds: Set<Long>,
        visibleRelations: List<Relation>,
    ): Map<Long, Pair<Float, Float>> {
        // BFS from center assigning generation levels.
        // PARENT_CHILD stores fromId=parent, toId=child, so fromId is 1 level above toId.
        val levelMap = mutableMapOf(centerId to 0)
        val queue = ArrayDeque<Long>()
        queue.add(centerId)
        while (queue.isNotEmpty()) {
            val nodeId = queue.removeFirst()
            val nodeLevel = levelMap[nodeId] ?: continue
            for (rel in visibleRelations) {
                val delta = verticalDelta(rel.typeKey)
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

        // Barycentric reordering: alternate top-down and bottom-up passes to reduce crossings.
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

    private fun verticalDelta(typeKey: String): Int = when (typeKey) {
        "PARENT_CHILD", "STEP_PARENT_CHILD", "UNCLE_AUNT", "GUARDIAN" -> 1
        "GRANDPARENT_GRANDCHILD" -> 2
        "MANAGER_REPORT", "MENTOR_MENTEE", "EMPLOYER_EMPLOYEE" -> 1
        else -> 0
    }
}
