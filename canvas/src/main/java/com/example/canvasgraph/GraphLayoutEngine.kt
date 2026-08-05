package com.example.canvasgraph

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

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

/**
 * Force-directed (Fruchterman–Reingold) layout engine for undirected cluster graphs.
 * Finds connected components, runs a spring simulation within each, then arranges
 * the settled components in a row-major grid.
 * Suitable for category-scoped graphs where there is no natural root or hierarchy.
 */
class ForceDirectedLayoutEngine : GraphLayoutEngine {
    override fun computePositions(
        nodeIds: Set<Long>,
        edges: List<LayoutEdge>,
        rootId: Long?,
    ): Map<Long, Pair<Float, Float>> {
        if (nodeIds.isEmpty()) return emptyMap()
        val components = findConnectedComponents(nodeIds.toList(), edges)
        val settled = components.map { component ->
            val compSet = component.toHashSet()
            simulateComponent(
                component,
                edges.filter { it.fromId in compSet && it.toId in compSet },
            )
        }
        return arrangeComponents(settled)
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

    @Suppress("NestedBlockDepth")
    private fun simulateComponent(
        nodeIds: List<Long>,
        edges: List<LayoutEdge>,
    ): Map<Long, Pair<Float, Float>> {
        val n = nodeIds.size
        if (n == 1) return mapOf(nodeIds[0] to (0f to 0f))

        // Standard FR: k scales with virtual area / n so larger components stay compact.
        val k = (VIRTUAL_CANVAS_SIDE / sqrt(n.toFloat())).coerceIn(MIN_K, MAX_K)

        val indexMap = nodeIds.withIndex().associate { (i, id) -> id to i }
        val px = FloatArray(n)
        val py = FloatArray(n)

        // Deterministic circular initial placement
        for (i in 0 until n) {
            val angle = (2.0 * PI * i / n - PI / 2).toFloat()
            val r = (n * k / (2.0 * PI)).toFloat().coerceAtLeast(k)
            px[i] = r * cos(angle)
            py[i] = r * sin(angle)
        }

        if (edges.isEmpty()) {
            return nodeIds.mapIndexed { i, id -> id to (px[i] to py[i]) }.toMap()
        }

        val dx = FloatArray(n)
        val dy = FloatArray(n)
        var temp = k * INITIAL_TEMP_FACTOR

        repeat(ITERATIONS) {
            dx.fill(0f)
            dy.fill(0f)

            // Repulsion between every pair of nodes
            for (i in 0 until n) {
                for (j in i + 1 until n) {
                    val rx = px[i] - px[j]
                    val ry = py[i] - py[j]
                    val dist = sqrt(rx * rx + ry * ry).coerceAtLeast(MIN_DIST)
                    val force = k * k / dist
                    val nx = rx / dist * force
                    val ny = ry / dist * force
                    dx[i] += nx
                    dy[i] += ny
                    dx[j] -= nx
                    dy[j] -= ny
                }
            }

            // Attraction along each edge
            for (edge in edges) {
                val i = indexMap[edge.fromId] ?: continue
                val j = indexMap[edge.toId] ?: continue
                val rx = px[i] - px[j]
                val ry = py[i] - py[j]
                val dist = sqrt(rx * rx + ry * ry).coerceAtLeast(MIN_DIST)
                val force = dist * dist / k
                val nx = rx / dist * force
                val ny = ry / dist * force
                dx[i] -= nx
                dy[i] -= ny
                dx[j] += nx
                dy[j] += ny
            }

            // Apply displacements capped by current temperature
            for (i in 0 until n) {
                val dispLen = sqrt(dx[i] * dx[i] + dy[i] * dy[i]).coerceAtLeast(MIN_DIST)
                val scale = minOf(dispLen, temp) / dispLen
                px[i] += dx[i] * scale
                py[i] += dy[i] * scale
            }

            temp *= COOLING
        }

        // Re-centre so that the bounding-box midpoint sits at the origin
        val cxMean = px.average().toFloat()
        val cyMean = py.average().toFloat()
        for (i in 0 until n) {
            px[i] -= cxMean
            py[i] -= cyMean
        }

        return nodeIds.mapIndexed { i, id -> id to (px[i] to py[i]) }.toMap()
    }

    private fun arrangeComponents(settled: List<Map<Long, Pair<Float, Float>>>): Map<Long, Pair<Float, Float>> {
        val result = mutableMapOf<Long, Pair<Float, Float>>()

        // Half-extents for each component (positions are centred at origin after simulation)
        val extW = settled.map { pos ->
            if (pos.isEmpty()) 0f else pos.values.maxOf { abs(it.first) } + NODE_HALF_W
        }
        val extH = settled.map { pos ->
            if (pos.isEmpty()) 0f else pos.values.maxOf { abs(it.second) } + NODE_HALF_H
        }

        // Largest components first
        val order = settled.indices.sortedByDescending { settled[it].size }

        var curX = 0f
        var curY = 0f
        var rowMaxH = 0f
        var rowCount = 0

        for (idx in order) {
            val pos = settled[idx]
            val ew = extW[idx]
            val eh = extH[idx]

            pos.forEach { (id, xy) ->
                result[id] = (xy.first + curX + ew) to (xy.second + curY + eh)
            }

            rowMaxH = maxOf(rowMaxH, eh * 2 + COMPONENT_GAP)
            rowCount++
            if (rowCount >= MAX_PER_ROW) {
                curX = 0f
                curY += rowMaxH
                rowMaxH = 0f
                rowCount = 0
            } else {
                curX += ew * 2 + COMPONENT_GAP
            }
        }

        return result
    }

    companion object {
        // k = VIRTUAL_CANVAS_SIDE / sqrt(n), clamped to [MIN_K, MAX_K].
        // Larger components automatically get tighter node spacing.
        private const val VIRTUAL_CANVAS_SIDE = 900f
        private const val MIN_K = 120f
        private const val MAX_K = 220f
        private const val ITERATIONS = 200
        private const val COOLING = 0.95f
        private const val INITIAL_TEMP_FACTOR = 2f
        private const val MIN_DIST = 0.1f
        private const val NODE_HALF_W = 110f
        private const val NODE_HALF_H = 26f
        private const val COMPONENT_GAP = 100f
        private const val MAX_PER_ROW = 3
    }
}
