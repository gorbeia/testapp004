package com.example.testapp004.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.data.RelationRepository
import com.example.testapp004.model.Category
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class CanvasPersonNode(
    val id: Long,
    val name: String,
    val x: Float,
    val y: Float,
    val dominantCategory: RelationCategory?,
    val isDirectMember: Boolean,
    val isNetSource: Boolean?,
    val distanceFromCategory: Int,
)

data class CanvasRelationEdge(
    val id: Long,
    val fromId: Long,
    val toId: Long,
    val label: String,
    val category: RelationCategory?,
    val isSymmetric: Boolean = false,
)

data class CategoryCanvasUiState(
    val categoryName: String = "",
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
class CategoryCanvasViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val acquaintanceRepository: AcquaintanceRepository,
    private val categoryRepository: CategoryRepository,
    private val relationRepository: RelationRepository,
) : ViewModel() {
    private val categoryId: Long = checkNotNull(savedStateHandle["categoryId"])

    private val _uiState = MutableStateFlow(CategoryCanvasUiState())
    val uiState: StateFlow<CategoryCanvasUiState> = _uiState.asStateFlow()

    private val relationDistanceFlow = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(
                acquaintanceRepository.acquaintances,
                categoryRepository.categories,
                relationRepository.relations,
                relationDistanceFlow,
            ) { acquaintances, categories, relations, distance ->
                val categoryName = categories.find { it.id == categoryId }?.name ?: ""
                val categoryTreeIds = descendantsAndSelf(categoryId, categories)

                val categoryPersonIds = acquaintances
                    .filter { person -> person.categoryIds.any { it in categoryTreeIds } }
                    .map { it.id }
                    .toSet()

                val distanceOneIds: Set<Long> = if (distance >= 1) {
                    relations
                        .filter { rel ->
                            (rel.fromId in categoryPersonIds) != (rel.toId in categoryPersonIds)
                        }
                        .flatMap { listOf(it.fromId, it.toId) }
                        .filter { it !in categoryPersonIds }
                        .toSet()
                } else {
                    emptySet()
                }

                val reachedSoFar = categoryPersonIds + distanceOneIds
                val distanceTwoIds: Set<Long> = if (distance >= 2) {
                    relations
                        .filter { rel ->
                            (rel.fromId in distanceOneIds && rel.toId !in reachedSoFar) ||
                                (rel.toId in distanceOneIds && rel.fromId !in reachedSoFar)
                        }
                        .flatMap { listOf(it.fromId, it.toId) }
                        .filter { it !in reachedSoFar }
                        .toSet()
                } else {
                    emptySet()
                }

                val distanceMap = buildMap<Long, Int> {
                    categoryPersonIds.forEach { put(it, 0) }
                    distanceOneIds.forEach { put(it, 1) }
                    distanceTwoIds.forEach { put(it, 2) }
                }
                val allPersonIds = distanceMap.keys

                val people = acquaintances.filter { it.id in allPersonIds }

                val visibleRelations = relations.filter { rel ->
                    rel.fromId in allPersonIds && rel.toId in allPersonIds
                }

                val components = findConnectedComponents(people.map { it.id }, visibleRelations)
                val nodePositions = computeLayout(components)

                val personCategoryLists = mutableMapOf<Long, MutableList<RelationCategory>>()
                val fromCounts = mutableMapOf<Long, Int>()
                val toCounts = mutableMapOf<Long, Int>()
                visibleRelations.forEach { rel ->
                    val cat = RelationTypes.findByKey(rel.typeKey)?.category ?: return@forEach
                    personCategoryLists.getOrPut(rel.fromId) { mutableListOf() }.add(cat)
                    personCategoryLists.getOrPut(rel.toId) { mutableListOf() }.add(cat)
                    fromCounts[rel.fromId] = (fromCounts[rel.fromId] ?: 0) + 1
                    toCounts[rel.toId] = (toCounts[rel.toId] ?: 0) + 1
                }

                CategoryCanvasUiState(
                    categoryName = categoryName,
                    relationDistance = distance,
                    nodes = people.map { person ->
                        val (x, y) = nodePositions[person.id] ?: (0f to 0f)
                        val dominant = personCategoryLists[person.id]
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
                            isDirectMember = categoryId in person.categoryIds,
                            isNetSource = isNetSource,
                            distanceFromCategory = distanceMap[person.id] ?: 0,
                        )
                    },
                    edges = visibleRelations.map { rel ->
                        val relType = RelationTypes.findByKey(rel.typeKey)
                        CanvasRelationEdge(
                            id = rel.id,
                            fromId = rel.fromId,
                            toId = rel.toId,
                            label = rel.labelFor(rel.fromId),
                            category = relType?.category,
                            isSymmetric = relType?.isSymmetric ?: false,
                        )
                    },
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

    private fun descendantsAndSelf(id: Long, categories: List<Category>): Set<Long> {
        val result = mutableSetOf(id)
        val queue = ArrayDeque<Long>()
        queue.add(id)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            categories.filter { it.parentId == current }.forEach { child ->
                if (result.add(child.id)) queue.add(child.id)
            }
        }
        return result
    }

    private fun findConnectedComponents(
        nodeIds: List<Long>,
        edges: List<Relation>,
    ): List<List<Long>> {
        val parent = nodeIds.associateWith { it }.toMutableMap()

        fun find(x: Long): Long {
            var root = x
            while (parent[root] != root) root = parent[root]!!
            var curr = x
            while (curr != root) {
                val next = parent[curr]!!
                parent[curr] = root
                curr = next
            }
            return root
        }

        edges.forEach { edge ->
            val ra = find(edge.fromId)
            val rb = find(edge.toId)
            if (ra != rb) parent[ra] = rb
        }

        return nodeIds.groupBy { find(it) }.values.toList()
    }

    private fun computeLayout(components: List<List<Long>>): Map<Long, Pair<Float, Float>> {
        val positions = mutableMapOf<Long, Pair<Float, Float>>()
        val nodeRadius = 50f
        val clusterGap = 100f
        val maxPerRow = 3

        val sorted = components.sortedByDescending { it.size }

        var curX = nodeRadius + clusterGap
        var curY = nodeRadius + clusterGap
        var rowMaxHeight = 0f
        var rowCount = 0

        sorted.forEach { component ->
            val n = component.size
            val clusterRadius = if (n == 1) {
                0f
            } else {
                (n * (nodeRadius * 2 + 30f) / (2 * PI)).toFloat().coerceAtLeast(nodeRadius * 2)
            }

            component.forEachIndexed { index, nodeId ->
                val angle = (2 * PI * index / n - PI / 2).toFloat()
                val x = curX + clusterRadius + if (n == 1) 0f else clusterRadius * cos(angle)
                val y = curY + clusterRadius + if (n == 1) 0f else clusterRadius * sin(angle)
                positions[nodeId] = x to y
            }

            val span = (clusterRadius + nodeRadius) * 2
            rowMaxHeight = maxOf(rowMaxHeight, span)
            rowCount++

            if (rowCount >= maxPerRow) {
                curX = nodeRadius + clusterGap
                curY += rowMaxHeight + clusterGap
                rowMaxHeight = 0f
                rowCount = 0
            } else {
                curX += span + clusterGap
            }
        }

        return positions
    }
}
