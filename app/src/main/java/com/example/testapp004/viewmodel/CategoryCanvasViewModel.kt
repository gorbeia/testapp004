package com.example.testapp004.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.testapp004.data.AcquaintanceRepository
import com.example.testapp004.data.CategoryRepository
import com.example.testapp004.data.RelationRepository
import com.example.testapp004.model.Category
import com.example.testapp004.model.Relation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
)

data class CanvasRelationEdge(
    val id: Long,
    val fromId: Long,
    val toId: Long,
    val label: String,
)

data class CategoryCanvasUiState(
    val categoryName: String = "",
    val nodes: List<CanvasPersonNode> = emptyList(),
    val edges: List<CanvasRelationEdge> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
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

    init {
        viewModelScope.launch {
            combine(
                acquaintanceRepository.acquaintances,
                categoryRepository.categories,
                relationRepository.relations,
            ) { acquaintances, categories, relations ->
                val categoryName = categories.find { it.id == categoryId }?.name ?: ""
                val categoryIds = descendantsAndSelf(categoryId, categories)

                val people = acquaintances.filter { person ->
                    person.categoryIds.any { it in categoryIds }
                }
                val peopleIds = people.map { it.id }.toSet()

                val intraRelations = relations.filter { rel ->
                    rel.fromId in peopleIds && rel.toId in peopleIds
                }

                val components = findConnectedComponents(people.map { it.id }, intraRelations)
                val nodePositions = computeLayout(components)

                CategoryCanvasUiState(
                    categoryName = categoryName,
                    nodes = people.map { person ->
                        val (x, y) = nodePositions[person.id] ?: (0f to 0f)
                        CanvasPersonNode(id = person.id, name = person.name, x = x, y = y)
                    },
                    edges = intraRelations.map { rel ->
                        CanvasRelationEdge(
                            id = rel.id,
                            fromId = rel.fromId,
                            toId = rel.toId,
                            label = rel.label,
                        )
                    },
                    isLoading = false,
                )
            }.collect { _uiState.value = it }
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

        // Larger clusters first, isolated nodes last
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
