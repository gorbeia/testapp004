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
