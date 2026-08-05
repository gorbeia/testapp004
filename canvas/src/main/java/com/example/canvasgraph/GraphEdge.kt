package com.example.canvasgraph

import androidx.compose.ui.graphics.Color

/**
 * Visual style for a rendered graph edge. All colors are pre-resolved by the caller.
 */
data class EdgeStyle(
    val edgeColor: Color,
    val labelColor: Color,
    /** When non-null, a filled rounded rect is drawn behind the edge label for legibility. */
    val labelBgColor: Color? = null,
)

/**
 * A directed (or symmetric) edge to be rendered in the relation graph.
 */
data class GraphEdge(
    val id: Long,
    val fromId: Long,
    val toId: Long,
    val label: String,
    val style: EdgeStyle,
    /** When true no arrowhead is drawn — the relationship has no inherent direction. */
    val isSymmetric: Boolean = false,
)
