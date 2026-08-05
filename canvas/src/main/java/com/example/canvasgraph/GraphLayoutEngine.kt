package com.example.canvasgraph

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A generic directed edge used exclusively for layout computation.
 * [verticalWeight] controls how many layers separate two nodes in
 * the hierarchical layout (0 = same layer, 1 = adjacent layers, etc.).
 * The caller maps app-specific relation types to this value before
 * invoking the layout engine.
 */
data class LayoutEdge(
    val fromId: Long,
    val toId: Long,
    val verticalWeight: Int = 0,
)

/** Computes 2-D positions for a set of graph nodes. */
interface GraphLayoutEngine {
    fun computePositions(
        nodeIds: Set<Long>,
        edges: List<LayoutEdge>,
        rootId: Long? = null,
    ): Map<Long, Pair<Float, Float>>
}

/**
 * Groups nodes into connected components and arranges each cluster in a circle.
 * Suitable for category-scoped graphs where there is no natural root or hierarchy.
 */
class RadialLayoutEngine : GraphLayoutEngine {
    override fun computePositions(
        nodeIds: Set<Long>,
        edges: List<LayoutEdge>,
        rootId: Long?,
    ): Map<Long, Pair<Float, Float>> {
        val components = findConnectedComponents(nodeIds.toList(), edges)
        return placeComponents(components)
    }

    private fun findConnectedComponents(nodeIds: List<Long>, edges: List<LayoutEdge>): List<List<Long>> {
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
            if (edge.fromId in parent && edge.toId in parent) {
                val ra = find(edge.fromId)
                val rb = find(edge.toId)
                if (ra != rb) parent[ra] = rb
            }
        }
        return nodeIds.groupBy { find(it) }.values.toList()
    }

    private fun placeComponents(components: List<List<Long>>): Map<Long, Pair<Float, Float>> {
        val positions = mutableMapOf<Long, Pair<Float, Float>>()
        val nodeRadius = 50f
        val clusterGap = 100f
        val maxPerRow = 3

        var curX = nodeRadius + clusterGap
        var curY = nodeRadius + clusterGap
        var rowMaxHeight = 0f
        var rowCount = 0

        components.sortedByDescending { it.size }.forEach { component ->
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

/**
 * Assigns nodes to horizontal layers using BFS from a root, guided by [LayoutEdge.verticalWeight].
 * Within each layer, node order is optimised to minimise edge crossings (exact permutation for
 * small layers ≤ 6 nodes, adjacent-swap heuristic otherwise).
 */
class HierarchicalLayoutEngine : GraphLayoutEngine {
    override fun computePositions(
        nodeIds: Set<Long>,
        edges: List<LayoutEdge>,
        rootId: Long?,
    ): Map<Long, Pair<Float, Float>> {
        val centerId = rootId ?: return emptyMap()
        return computeHierarchicalPositions(centerId, nodeIds, edges)
    }

    private fun computeHierarchicalPositions(
        centerId: Long,
        visibleIds: Set<Long>,
        visibleEdges: List<LayoutEdge>,
    ): Map<Long, Pair<Float, Float>> {
        val levelMap = mutableMapOf(centerId to 0)
        val queue = ArrayDeque<Long>()
        queue.add(centerId)
        while (queue.isNotEmpty()) {
            val nodeId = queue.removeFirst()
            val nodeLevel = levelMap[nodeId] ?: continue
            for (edge in visibleEdges) {
                val delta = edge.verticalWeight
                when {
                    edge.fromId == nodeId && edge.toId !in levelMap -> {
                        levelMap[edge.toId] = nodeLevel - delta
                        queue.add(edge.toId)
                    }
                    edge.toId == nodeId && edge.fromId !in levelMap -> {
                        levelMap[edge.fromId] = nodeLevel + delta
                        queue.add(edge.fromId)
                    }
                }
            }
        }
        visibleIds.forEach { id -> levelMap.getOrPut(id) { 0 } }

        val levelGroups = levelMap.entries.groupBy({ it.value }, { it.key })
        val allLevels = levelGroups.keys.sorted()
        val layerHeight = 170f
        val baseSpacing = 220f
        val maxNodesInLayer = levelGroups.values.maxOfOrNull { it.size } ?: 1
        val nodeSpacing = baseSpacing * (4f / maxNodesInLayer.coerceAtLeast(4)).coerceIn(0.7f, 1.3f)
        val positions = mutableMapOf<Long, Pair<Float, Float>>()

        for ((level, ids) in levelGroups) {
            val sorted = ids.sortedWith(
                compareBy(
                    { baryScore(it, level, levelMap, visibleEdges, positions) },
                    { upperNeighborCount(it, level, levelMap, visibleEdges) },
                    { -lowerNeighborCount(it, level, levelMap, visibleEdges) },
                    { it },
                ),
            )
            val n = sorted.size
            sorted.forEachIndexed { i, id ->
                positions[id] = (-(n - 1) / 2f + i) * nodeSpacing to -level * layerHeight
            }
        }

        repeat(4) { pass ->
            val levelOrder = if (pass % 2 == 0) allLevels else allLevels.reversed()
            for (level in levelOrder) {
                val ids = levelGroups[level] ?: continue
                if (ids.size <= 1) continue
                val ordered = optimizeLayerOrder(ids, level, levelMap, visibleEdges, positions)
                val n = ordered.size
                ordered.forEachIndexed { i, id ->
                    val y = positions[id]?.second ?: (-level * layerHeight)
                    positions[id] = (-(n - 1) / 2f + i) * nodeSpacing to y
                }
            }
        }

        val cx = positions[centerId]?.first ?: 0f
        val cy = positions[centerId]?.second ?: 0f
        return positions.mapValues { (_, pos) -> (pos.first - cx) to (pos.second - cy) }
    }

    private fun baryScore(
        id: Long,
        level: Int,
        levelMap: Map<Long, Int>,
        edges: List<LayoutEdge>,
        positions: Map<Long, Pair<Float, Float>>,
    ): Float {
        val xs = edges.mapNotNull { edge ->
            val neighbor = when {
                edge.fromId == id -> edge.toId
                edge.toId == id -> edge.fromId
                else -> null
            }
            neighbor?.takeIf { levelMap[it] != level }?.let { positions[it]?.first }
        }
        return if (xs.isEmpty()) positions[id]?.first ?: 0f else xs.average().toFloat()
    }

    private fun upperNeighborCount(
        id: Long,
        level: Int,
        levelMap: Map<Long, Int>,
        edges: List<LayoutEdge>,
    ): Int = edges.count { edge ->
        when {
            edge.fromId == id -> levelMap.getOrDefault(edge.toId, level) > level
            edge.toId == id -> levelMap.getOrDefault(edge.fromId, level) > level
            else -> false
        }
    }

    private fun lowerNeighborCount(
        id: Long,
        level: Int,
        levelMap: Map<Long, Int>,
        edges: List<LayoutEdge>,
    ): Int = edges.count { edge ->
        when {
            edge.fromId == id -> levelMap.getOrDefault(edge.toId, level) < level
            edge.toId == id -> levelMap.getOrDefault(edge.fromId, level) < level
            else -> false
        }
    }

    private fun countCrossings(
        permuted: List<Long>,
        fixed: List<Long>,
        edges: List<LayoutEdge>,
    ): Int {
        val permPos = permuted.withIndex().associate { (i, id) -> id to i }
        val fixedPos = fixed.withIndex().associate { (i, id) -> id to i }
        val layerEdges = edges.mapNotNull { edge ->
            val p = permPos[edge.fromId] ?: permPos[edge.toId]
            val f = fixedPos[edge.toId] ?: fixedPos[edge.fromId]
            if (p != null && f != null) p to f else null
        }
        var crossings = 0
        for (i in layerEdges.indices) {
            for (j in i + 1 until layerEdges.size) {
                val (p1, f1) = layerEdges[i]
                val (p2, f2) = layerEdges[j]
                if ((p1 - p2) * (f1 - f2) < 0) crossings++
            }
        }
        return crossings
    }

    private fun permutations(ids: List<Long>): Sequence<List<Long>> = sequence {
        if (ids.size <= 1) {
            yield(ids)
            return@sequence
        }
        val arr = ids.toMutableList()
        val c = IntArray(arr.size)
        yield(arr.toList())
        var i = 0
        while (i < arr.size) {
            if (c[i] < i) {
                if (i % 2 == 0) {
                    val tmp = arr[0]
                    arr[0] = arr[i]
                    arr[i] = tmp
                } else {
                    val tmp = arr[c[i]]
                    arr[c[i]] = arr[i]
                    arr[i] = tmp
                }
                yield(arr.toList())
                c[i]++
                i = 0
            } else {
                c[i] = 0
                i++
            }
        }
    }

    private fun adjacentSwapOptimize(
        ids: List<Long>,
        aboveSorted: List<Long>,
        belowSorted: List<Long>,
        edges: List<LayoutEdge>,
        naturalRank: Map<Long, Int>,
    ): List<Long> {
        val arr = ids.toMutableList()

        fun crossings() = countCrossings(arr, aboveSorted, edges) + countCrossings(arr, belowSorted, edges)

        var improved = true
        while (improved) {
            improved = false
            for (i in 0 until arr.size - 1) {
                val before = crossings()
                val tmp = arr[i]
                arr[i] = arr[i + 1]
                arr[i + 1] = tmp
                val after = crossings()
                val leftRankAfter = naturalRank.getOrDefault(arr[i], 0)
                val rightRankAfter = naturalRank.getOrDefault(arr[i + 1], 0)
                val keepSwap = after < before || (after == before && leftRankAfter < rightRankAfter)
                if (!keepSwap) {
                    val t = arr[i]
                    arr[i] = arr[i + 1]
                    arr[i + 1] = t
                } else {
                    improved = true
                }
            }
        }
        return arr
    }

    private fun optimizeLayerOrder(
        ids: List<Long>,
        level: Int,
        levelMap: Map<Long, Int>,
        edges: List<LayoutEdge>,
        positions: Map<Long, Pair<Float, Float>>,
    ): List<Long> {
        val aboveSorted = levelMap.entries
            .filter { it.value > level }
            .map { it.key }
            .sortedBy { positions[it]?.first ?: 0f }
        val belowSorted = levelMap.entries
            .filter { it.value < level }
            .map { it.key }
            .sortedBy { positions[it]?.first ?: 0f }

        val naturalOrder = ids.sortedWith(
            compareBy(
                { baryScore(it, level, levelMap, edges, positions) },
                { upperNeighborCount(it, level, levelMap, edges) },
                { -lowerNeighborCount(it, level, levelMap, edges) },
                { it },
            ),
        )
        val naturalRank = naturalOrder.withIndex().associate { (i, id) -> id to i }

        fun crossingsForCandidate(candidate: List<Long>): Int =
            countCrossings(candidate, aboveSorted, edges) + countCrossings(candidate, belowSorted, edges)

        return if (ids.size <= 6) {
            permutations(ids)
                .minWithOrNull(
                    compareBy(
                        { crossingsForCandidate(it) },
                        { it.map { id -> naturalRank[id]!! }.joinToString(",") },
                    ),
                ) ?: ids
        } else {
            adjacentSwapOptimize(naturalOrder, aboveSorted, belowSorted, edges, naturalRank)
        }
    }
}
