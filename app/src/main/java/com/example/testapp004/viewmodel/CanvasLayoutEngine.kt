package com.example.testapp004.viewmodel

import com.example.testapp004.model.Relation
import com.example.testapp004.model.RelationTypes
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal interface CanvasLayoutEngine {
    fun computePositions(
        nodeIds: Set<Long>,
        edges: List<Relation>,
        rootId: Long? = null,
    ): Map<Long, Pair<Float, Float>>
}

internal class RadialClusterLayoutEngine : CanvasLayoutEngine {
    override fun computePositions(
        nodeIds: Set<Long>,
        edges: List<Relation>,
        rootId: Long?,
    ): Map<Long, Pair<Float, Float>> {
        val components = findConnectedComponents(nodeIds.toList(), edges)
        return placeComponents(components)
    }

    private fun findConnectedComponents(nodeIds: List<Long>, edges: List<Relation>): List<List<Long>> {
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

internal class HierarchicalLayoutEngine : CanvasLayoutEngine {
    override fun computePositions(
        nodeIds: Set<Long>,
        edges: List<Relation>,
        rootId: Long?,
    ): Map<Long, Pair<Float, Float>> {
        val centerId = rootId ?: return emptyMap()
        return computeHierarchicalPositions(centerId, nodeIds, edges)
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
        val baseSpacing = 220f
        val maxNodesInLayer = levelGroups.values.maxOfOrNull { it.size } ?: 1
        val nodeSpacing = baseSpacing * (4f / maxNodesInLayer.coerceAtLeast(4)).coerceIn(0.7f, 1.3f)
        val positions = mutableMapOf<Long, Pair<Float, Float>>()

        for ((level, ids) in levelGroups) {
            val sorted = ids.sortedWith(
                compareBy(
                    { baryScore(it, level, levelMap, visibleRelations, positions) },
                    { upperNeighborCount(it, level, levelMap, visibleRelations) },
                    { -lowerNeighborCount(it, level, levelMap, visibleRelations) },
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
                val ordered = optimizeLayerOrder(ids, level, levelMap, visibleRelations, positions)
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
        edges: List<Relation>,
        positions: Map<Long, Pair<Float, Float>>,
    ): Float {
        val xs = edges.mapNotNull { rel ->
            val neighbor = when {
                rel.fromId == id -> rel.toId
                rel.toId == id -> rel.fromId
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
        edges: List<Relation>,
    ): Int = edges.count { rel ->
        when {
            rel.fromId == id -> levelMap.getOrDefault(rel.toId, level) > level
            rel.toId == id -> levelMap.getOrDefault(rel.fromId, level) > level
            else -> false
        }
    }

    private fun lowerNeighborCount(
        id: Long,
        level: Int,
        levelMap: Map<Long, Int>,
        edges: List<Relation>,
    ): Int = edges.count { rel ->
        when {
            rel.fromId == id -> levelMap.getOrDefault(rel.toId, level) < level
            rel.toId == id -> levelMap.getOrDefault(rel.fromId, level) < level
            else -> false
        }
    }

    private fun countCrossings(
        permuted: List<Long>,
        fixed: List<Long>,
        edges: List<Relation>,
    ): Int {
        val permPos = permuted.withIndex().associate { (i, id) -> id to i }
        val fixedPos = fixed.withIndex().associate { (i, id) -> id to i }
        val layerEdges = edges.mapNotNull { rel ->
            val p = permPos[rel.fromId] ?: permPos[rel.toId]
            val f = fixedPos[rel.toId] ?: fixedPos[rel.fromId]
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
        edges: List<Relation>,
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
        edges: List<Relation>,
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
