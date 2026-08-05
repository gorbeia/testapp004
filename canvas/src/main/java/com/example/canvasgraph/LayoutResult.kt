package com.example.canvasgraph

data class LayoutResult(
    val positions: Map<Long, Pair<Float, Float>>,
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
) {
    companion object {
        val Empty = LayoutResult(emptyMap(), 0f, 0f, 0f, 0f)
    }
}

internal fun Map<Long, Pair<Float, Float>>.toLayoutResult(): LayoutResult {
    if (isEmpty()) return LayoutResult.Empty
    val minX = values.minOf { it.first }
    val minY = values.minOf { it.second }
    val maxX = values.maxOf { it.first }
    val maxY = values.maxOf { it.second }
    return LayoutResult(this, minX, minY, maxX, maxY)
}
