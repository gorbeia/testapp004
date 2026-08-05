package com.example.canvasgraph

import androidx.compose.ui.graphics.Color

/**
 * Visual style for a rendered graph node. All colors must be fully resolved
 * by the caller before constructing this — the library has no knowledge of
 * app-specific semantics (relation categories, membership distance, etc.).
 */
data class NodeStyle(
    val fillColor: Color,
    val strokeColor: Color,
    val textColor: Color,
    val useDashedBorder: Boolean = false,
    /** When non-null, an outer ring is drawn around the node using this color at 35% opacity. */
    val ringColor: Color? = null,
)

/**
 * A node to be rendered in the relation graph. Positions are in graph space;
 * the composable handles pan/zoom into screen space.
 */
data class GraphNode(
    val id: Long,
    val name: String,
    val x: Float,
    val y: Float,
    val style: NodeStyle,
    /** When true the node receives a prominent ring to mark it as the focal point. */
    val isCenterNode: Boolean = false,
)
