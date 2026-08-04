package com.example.testapp004.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.data.RelationRepository
import com.example.testapp004.model.Relation
import com.example.testapp004.model.RelationCategory
import com.example.testapp004.model.RelationTypes
import com.example.testapp004.model.descendantsAndSelf
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
    val relationCategoryFilter: Set<RelationCategory> = emptySet(),
    val isRelationDialogOpen: Boolean = false,
    val pendingRelationFromId: Long? = null,
    val pendingRelationToId: Long? = null,
    val pendingRelationFromName: String = "",
    val pendingRelationToName: String = "",
)

private data class CanvasFilters(val distance: Int, val categoryFilter: Set<RelationCategory>)

@HiltViewModel
class CategoryCanvasViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val acquaintanceRepository: AcquaintanceRepository,
    private val categoryRepository: CategoryRepository,
    relationRepository: RelationRepository,
) : CanvasViewModel(relationRepository) {
    private val categoryId: Long = checkNotNull(savedStateHandle["categoryId"])

    private val _uiState = MutableStateFlow(CategoryCanvasUiState())
    val uiState: StateFlow<CategoryCanvasUiState> = _uiState.asStateFlow()

    private val relationDistanceFlow = MutableStateFlow(0)
    private val relationCategoryFilterFlow = MutableStateFlow(emptySet<RelationCategory>())
    private val filtersFlow = combine(
        relationDistanceFlow,
        relationCategoryFilterFlow,
    ) { distance, categoryFilter -> CanvasFilters(distance, categoryFilter) }

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
                categoryRepository.categories,
                relationRepository.relations,
                filtersFlow,
            ) { acquaintances, categories, relations, filters ->
                val distance = filters.distance
                val categoryFilter = filters.categoryFilter
                val categoryName = categories.find { it.id == categoryId }?.name ?: ""
                val categoryTreeIds = categories.descendantsAndSelf(categoryId)

                val categoryPersonIds = acquaintances
                    .filter { person -> person.categoryIds.any { it in categoryTreeIds } }
                    .map { it.id }
                    .toSet()

                val filteredRelations = if (categoryFilter.isEmpty()) {
                    relations
                } else {
                    relations.filter { rel -> RelationTypes.findByKey(rel.typeKey)?.category in categoryFilter }
                }

                val distanceOneIds: Set<Long> = if (distance >= 1) {
                    filteredRelations
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
                    filteredRelations
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

                val visibleRelations = filteredRelations.filter { rel ->
                    rel.fromId in allPersonIds && rel.toId in allPersonIds
                }

                val components = findConnectedComponents(allPersonIds.toList(), visibleRelations)
                val nodePositions = computeLayout(components)

                val nodes = buildCanvasNodes(
                    acquaintances = acquaintances,
                    visibleIds = allPersonIds,
                    positions = nodePositions,
                    distanceMap = distanceMap,
                    visibleRelations = visibleRelations,
                    isDirectMember = { id ->
                        acquaintances.find { it.id == id }?.categoryIds?.any { it == categoryId } == true
                    },
                )
                val edges = buildCanvasEdges(visibleRelations)

                CategoryCanvasUiState(
                    categoryName = categoryName,
                    relationDistance = distance,
                    relationCategoryFilter = categoryFilter,
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

    fun setRelationDistance(d: Int) {
        relationDistanceFlow.value = d.coerceIn(0, 2)
    }

    fun toggleRelationCategoryFilter(category: RelationCategory) {
        val current = relationCategoryFilterFlow.value
        relationCategoryFilterFlow.value = if (category in current) current - category else current + category
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
