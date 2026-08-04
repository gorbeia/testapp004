package com.example.testapp004.viewmodel

import com.example.testapp004.model.Acquaintance
import com.example.testapp004.model.Relation
import com.example.testapp004.model.RelationCategory
import com.example.testapp004.model.RelationTypes
import com.example.testapp004.model.labelFor

internal fun buildCanvasEdges(visibleRelations: List<Relation>): List<CanvasRelationEdge> =
    visibleRelations.map { rel ->
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

internal fun buildCanvasNodes(
    acquaintances: List<Acquaintance>,
    visibleIds: Set<Long>,
    positions: Map<Long, Pair<Float, Float>>,
    distanceMap: Map<Long, Int>,
    visibleRelations: List<Relation>,
    isDirectMember: (Long) -> Boolean,
): List<CanvasPersonNode> {
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
    return acquaintances.filter { it.id in visibleIds }.mapNotNull { person ->
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
            isDirectMember = isDirectMember(person.id),
            isNetSource = isNetSource,
            distanceFromCategory = distanceMap[person.id] ?: 0,
        )
    }
}
