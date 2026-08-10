package com.example.canvasgraph

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val NODE_HALF_H = 26f
private const val NODE_MAX_HALF_W = 110f
private const val NODE_H_PAD = 18f
private const val ARROW_LEN = 22f
private const val ARROW_HALF_ANGLE = 0.4f
private const val ARC_BEND = 60f
private const val ARC_LABEL_OFFSET = 16f

/**
 * A pan/zoom/drag-to-relate graph composable with no app-specific knowledge.
 * All visual styling (colors, border style, center rings) is carried by
 * [GraphNode.style] and [GraphEdge.style]; the only graph-level color
 * the caller may wish to override is [dropTargetHighlightColor].
 *
 * Gesture behaviour:
 * - Pinch / two-finger drag → pan and zoom
 * - Single tap on a node → [onNodeTap]
 * - Long-press drag from one node onto another → [onRelationDrop]
 *
 * Node positions animate smoothly with a low-stiffness spring whenever the
 * [nodes] list changes coordinates (e.g. when switching layout engines).
 * Pass an external [viewportState] to read or override zoom and pan from outside.
 */
@Composable
fun RelationGraph(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    onNodeTap: (id: Long) -> Unit = {},
    onRelationDrop: (fromId: Long, toId: Long) -> Unit = { _, _ -> },
    dropTargetHighlightColor: Color = MaterialTheme.colorScheme.tertiary,
    nodeTextStyle: TextStyle = MaterialTheme.typography.labelMedium,
    viewportState: GraphViewportState = rememberGraphViewportState(),
    forceArcs: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val nodeMap = remember(nodes) { nodes.associateBy { it.id } }

    var canvasSize by remember { mutableStateOf(Size.Zero) }

    var isDragging by remember { mutableStateOf(false) }
    var draggedNodeId by remember { mutableStateOf<Long?>(null) }
    var dragScreenPos by remember { mutableStateOf(Offset.Zero) }
    var dropTargetId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(nodes, canvasSize) {
        if (nodes.isEmpty() || canvasSize == Size.Zero) return@LaunchedEffect
        if (viewportState.zoom != 1f || viewportState.panOffset != Offset.Zero) return@LaunchedEffect
        val padding = 80f
        val minX = nodes.minOf { it.x } - NODE_MAX_HALF_W - padding
        val minY = nodes.minOf { it.y } - NODE_HALF_H - padding
        val maxX = nodes.maxOf { it.x } + NODE_MAX_HALF_W + padding
        val maxY = nodes.maxOf { it.y } + NODE_HALF_H + padding
        val contentW = maxX - minX
        val contentH = maxY - minY
        viewportState.zoom = minOf(canvasSize.width / contentW, canvasSize.height / contentH, 1.2f)
            .coerceAtLeast(0.35f)
        viewportState.panOffset = Offset(
            (canvasSize.width - contentW * viewportState.zoom) / 2f - minX * viewportState.zoom,
            (canvasSize.height - contentH * viewportState.zoom) / 2f - minY * viewportState.zoom,
        )
    }

    // Per-node Animatables that drive smooth position transitions when nodes change layout.
    val xAnimatables = remember { mutableMapOf<Long, Animatable<Float, AnimationVector1D>>() }
    val yAnimatables = remember { mutableMapOf<Long, Animatable<Float, AnimationVector1D>>() }
    LaunchedEffect(nodes) {
        val currentIds = nodes.map { it.id }.toSet()
        xAnimatables.keys.retainAll(currentIds)
        yAnimatables.keys.retainAll(currentIds)
        coroutineScope {
            nodes.forEach { node ->
                val xa = xAnimatables.getOrPut(node.id) { Animatable(node.x) }
                val ya = yAnimatables.getOrPut(node.id) { Animatable(node.y) }
                launch { xa.animateTo(node.x, spring(stiffness = Spring.StiffnessLow)) }
                launch { ya.animateTo(node.y, spring(stiffness = Spring.StiffnessLow)) }
            }
        }
    }

    // Reading animatable .value here registers snapshot observations that drive recomposition.
    val animatedNodes = nodes.map { node ->
        val x = xAnimatables[node.id]?.value ?: node.x
        val y = yAnimatables[node.id]?.value ?: node.y
        node.copy(x = x, y = y)
    }
    val animatedNodeMap = animatedNodes.associateBy { it.id }

    val textMeasurer = rememberTextMeasurer()
    val nodeHalfWidths = remember(nodes, textMeasurer, nodeTextStyle) {
        nodes.associate { node ->
            val m = textMeasurer.measure(
                text = node.name,
                style = nodeTextStyle,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints(maxWidth = ((NODE_MAX_HALF_W - NODE_H_PAD) * 2).toInt()),
            )
            node.id to (m.size.width / 2f + NODE_H_PAD).coerceIn(NODE_HALF_H, NODE_MAX_HALF_W)
        }
    }

    Canvas(
        modifier = modifier
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomChange, _ ->
                    if (!isDragging) {
                        val newZoom = (viewportState.zoom * zoomChange).coerceIn(0.1f, 5f)
                        viewportState.panOffset = centroid -
                            (centroid - viewportState.panOffset) * (newZoom / viewportState.zoom) + pan
                        viewportState.zoom = newZoom
                    }
                }
            }
            .pointerInput(nodes) {
                detectTapGestures { tapOffset ->
                    val vx = (tapOffset.x - viewportState.panOffset.x) / viewportState.zoom
                    val vy = (tapOffset.y - viewportState.panOffset.y) / viewportState.zoom
                    nodes.firstOrNull { node ->
                        val dx = vx - node.x
                        val dy = vy - node.y
                        abs(dx) <= (nodeHalfWidths[node.id] ?: NODE_MAX_HALF_W) &&
                            abs(dy) <= NODE_HALF_H
                    }?.let { onNodeTap(it.id) }
                }
            }
            .pointerInput(nodes) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val vx = (offset.x - viewportState.panOffset.x) / viewportState.zoom
                        val vy = (offset.y - viewportState.panOffset.y) / viewportState.zoom
                        val node = nodes.firstOrNull { n ->
                            abs(vx - n.x) <= (nodeHalfWidths[n.id] ?: NODE_MAX_HALF_W) &&
                                abs(vy - n.y) <= NODE_HALF_H
                        }
                        if (node != null) {
                            draggedNodeId = node.id
                            dragScreenPos = offset
                            isDragging = true
                        }
                    },
                    onDrag = { change, _ ->
                        if (isDragging) {
                            dragScreenPos = change.position
                            val vx = (change.position.x - viewportState.panOffset.x) / viewportState.zoom
                            val vy = (change.position.y - viewportState.panOffset.y) / viewportState.zoom
                            dropTargetId = nodes.firstOrNull { n ->
                                n.id != draggedNodeId &&
                                    abs(vx - n.x) <= (nodeHalfWidths[n.id] ?: NODE_MAX_HALF_W) &&
                                    abs(vy - n.y) <= NODE_HALF_H
                            }?.id
                            change.consume()
                        }
                    },
                    onDragEnd = {
                        if (isDragging) {
                            val from = draggedNodeId
                            val to = dropTargetId
                            if (from != null && to != null) onRelationDrop(from, to)
                            isDragging = false
                            draggedNodeId = null
                            dropTargetId = null
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        draggedNodeId = null
                        dropTargetId = null
                    },
                )
            },
    ) {
        withTransform({
            translate(viewportState.panOffset.x, viewportState.panOffset.y)
            scale(viewportState.zoom, viewportState.zoom, Offset.Zero)
        }) {
            // Fan out parallel edges between the same pair of nodes so they don't overlap.
            // Arc direction is canonicalised in the smaller-id → larger-id frame for consistency.
            val arcSides: Map<Long, Int> = buildMap {
                edges.groupBy { e ->
                    if (e.fromId < e.toId) e.fromId to e.toId else e.toId to e.fromId
                }.values.forEach { group ->
                    val n = group.size
                    group.forEachIndexed { i, edge ->
                        val arcIndex = when {
                            n > 1 -> 2 * i - (n - 1)
                            forceArcs -> 1
                            else -> 0
                        }
                        put(edge.id, if (edge.fromId <= edge.toId) arcIndex else -arcIndex)
                    }
                }
            }

            edges.forEach { edge ->
                val from = animatedNodeMap[edge.fromId] ?: return@forEach
                val to = animatedNodeMap[edge.toId] ?: return@forEach
                drawGraphEdge(
                    from = Offset(from.x, from.y),
                    to = Offset(to.x, to.y),
                    fromHalfW = nodeHalfWidths[edge.fromId] ?: NODE_MAX_HALF_W,
                    toHalfW = nodeHalfWidths[edge.toId] ?: NODE_MAX_HALF_W,
                    fromId = edge.fromId,
                    toId = edge.toId,
                    label = edge.label,
                    style = edge.style,
                    drawArrow = !edge.isSymmetric,
                    arcSide = arcSides[edge.id] ?: 0,
                    allNodes = animatedNodes,
                    allHalfWidths = nodeHalfWidths,
                    textMeasurer = textMeasurer,
                )
            }

            animatedNodes.forEach { node ->
                val isDropTarget = isDragging && node.id == dropTargetId
                val isDragSource = isDragging && node.id == draggedNodeId
                val hw = nodeHalfWidths[node.id] ?: NODE_MAX_HALF_W

                node.style.ringColor?.let { ringColor ->
                    val pad = 5f
                    drawRoundRect(
                        color = ringColor.copy(alpha = 0.35f),
                        topLeft = Offset(node.x - hw - pad, node.y - NODE_HALF_H - pad),
                        size = Size((hw + pad) * 2, (NODE_HALF_H + pad) * 2),
                        cornerRadius = CornerRadius(NODE_HALF_H + pad),
                        style = Stroke(width = 4f),
                    )
                }

                val resolvedStyle = when {
                    isDragSource -> node.style.copy(
                        fillColor = node.style.fillColor.copy(alpha = 0.3f),
                        strokeColor = node.style.strokeColor.copy(alpha = 0.3f),
                        textColor = node.style.textColor.copy(alpha = 0.3f),
                    )
                    isDropTarget -> node.style.copy(strokeColor = dropTargetHighlightColor)
                    else -> node.style
                }
                drawGraphNode(
                    center = Offset(node.x, node.y),
                    halfW = hw,
                    name = node.name,
                    style = resolvedStyle,
                    textMeasurer = textMeasurer,
                    nodeTextStyle = nodeTextStyle,
                )

                if (isDropTarget) {
                    val pad = 6f
                    drawRoundRect(
                        color = dropTargetHighlightColor,
                        topLeft = Offset(node.x - hw - pad, node.y - NODE_HALF_H - pad),
                        size = Size((hw + pad) * 2, (NODE_HALF_H + pad) * 2),
                        cornerRadius = CornerRadius(NODE_HALF_H + pad),
                        style = Stroke(width = 3.5f),
                    )
                }
            }

            if (isDragging) {
                val ghostNode = draggedNodeId?.let { animatedNodeMap[it] }
                if (ghostNode != null) {
                    val ghostCanvasX = (dragScreenPos.x - viewportState.panOffset.x) / viewportState.zoom
                    val ghostCanvasY = (dragScreenPos.y - viewportState.panOffset.y) / viewportState.zoom
                    val ghostStroke = if (dropTargetId != null) {
                        dropTargetHighlightColor
                    } else {
                        ghostNode.style.strokeColor
                    }
                    drawGraphNode(
                        center = Offset(ghostCanvasX, ghostCanvasY),
                        halfW = nodeHalfWidths[ghostNode.id] ?: NODE_MAX_HALF_W,
                        name = ghostNode.name,
                        style = ghostNode.style.copy(strokeColor = ghostStroke),
                        textMeasurer = textMeasurer,
                        nodeTextStyle = nodeTextStyle,
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGraphNode(
    center: Offset,
    halfW: Float,
    name: String,
    style: NodeStyle,
    textMeasurer: TextMeasurer,
    nodeTextStyle: TextStyle,
) {
    val topLeft = Offset(center.x - halfW, center.y - NODE_HALF_H)
    val size = Size(halfW * 2, NODE_HALF_H * 2)
    val corner = CornerRadius(NODE_HALF_H, NODE_HALF_H)
    drawRoundRect(color = style.fillColor, topLeft = topLeft, size = size, cornerRadius = corner)
    drawRoundRect(
        color = style.strokeColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = corner,
        style = Stroke(
            width = 2.5f,
            pathEffect = if (style.useDashedBorder) PathEffect.dashPathEffect(floatArrayOf(8f, 4f)) else null,
        ),
    )
    val measured = textMeasurer.measure(
        text = name,
        style = nodeTextStyle.copy(color = style.textColor),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = ((halfW - NODE_H_PAD) * 2).coerceAtLeast(1f).toInt()),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(center.x - measured.size.width / 2f, center.y - measured.size.height / 2f),
    )
}

private fun rectBorderPoint(cx: Float, cy: Float, ux: Float, uy: Float, hw: Float, hh: Float): Offset {
    val ax = abs(ux)
    val ay = abs(uy)
    val t = if (ax * hh >= ay * hw) hw / ax else hh / ay
    return Offset(cx + ux * t, cy + uy * t)
}

private fun segmentIntersectsNode(
    ax: Float,
    ay: Float,
    bx: Float,
    by: Float,
    cx: Float,
    cy: Float,
    hw: Float,
    hh: Float,
): Boolean {
    val dx = bx - ax
    val dy = by - ay
    val lenSq = dx * dx + dy * dy
    if (lenSq < 1f) return false
    val t = (((cx - ax) * dx + (cy - ay) * dy) / lenSq).coerceIn(0f, 1f)
    return abs(ax + t * dx - cx) < hw && abs(ay + t * dy - cy) < hh
}

@Suppress("LongParameterList")
private fun DrawScope.drawGraphEdge(
    from: Offset,
    to: Offset,
    fromHalfW: Float,
    toHalfW: Float,
    fromId: Long,
    toId: Long,
    label: String,
    style: EdgeStyle,
    drawArrow: Boolean,
    arcSide: Int,
    allNodes: List<GraphNode>,
    allHalfWidths: Map<Long, Float>,
    textMeasurer: TextMeasurer,
) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 1f) return

    val ux = dx / length
    val uy = dy / length
    val perpX = -uy
    val perpY = ux

    val start = rectBorderPoint(from.x, from.y, ux, uy, fromHalfW, NODE_HALF_H)
    val end = rectBorderPoint(to.x, to.y, -ux, -uy, toHalfW, NODE_HALF_H)

    val occluded = arcSide == 0 && allNodes.any { node ->
        node.id != fromId && node.id != toId &&
            segmentIntersectsNode(
                from.x, from.y, to.x, to.y,
                node.x, node.y,
                allHalfWidths[node.id] ?: NODE_MAX_HALF_W, NODE_HALF_H,
            )
    }
    val effectiveSide = when {
        arcSide != 0 -> arcSide
        occluded -> 1
        else -> 0
    }

    val labelPos: Offset
    val labelPerpX: Float
    val labelPerpY: Float
    val arrowDirX: Float
    val arrowDirY: Float

    if (effectiveSide == 0) {
        drawLine(color = style.edgeColor, start = start, end = end, strokeWidth = 2.5f, cap = StrokeCap.Round)
        arrowDirX = ux
        arrowDirY = uy
        labelPos = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
        labelPerpX = perpX
        labelPerpY = perpY
    } else {
        val bendAmount = ARC_BEND * effectiveSide
        val cpX = (from.x + to.x) / 2f + perpX * bendAmount
        val cpY = (from.y + to.y) / 2f + perpY * bendAmount
        drawPath(
            path = Path().apply {
                moveTo(start.x, start.y)
                quadraticBezierTo(cpX, cpY, end.x, end.y)
            },
            color = style.edgeColor,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round),
        )
        val tanX = end.x - cpX
        val tanY = end.y - cpY
        val tanLen = sqrt(tanX * tanX + tanY * tanY).coerceAtLeast(0.001f)
        arrowDirX = tanX / tanLen
        arrowDirY = tanY / tanLen
        val bendSign = if (effectiveSide > 0) 1f else -1f
        labelPos = Offset(
            0.25f * start.x + 0.5f * cpX + 0.25f * end.x + perpX * bendSign * ARC_LABEL_OFFSET,
            0.25f * start.y + 0.5f * cpY + 0.25f * end.y + perpY * bendSign * ARC_LABEL_OFFSET,
        )
        labelPerpX = 0f
        labelPerpY = 0f
    }

    if (drawArrow) {
        val angle = atan2(arrowDirY, arrowDirX)
        val a1 = Offset(
            end.x - ARROW_LEN * cos(angle - ARROW_HALF_ANGLE),
            end.y - ARROW_LEN * sin(angle - ARROW_HALF_ANGLE),
        )
        val a2 = Offset(
            end.x - ARROW_LEN * cos(angle + ARROW_HALF_ANGLE),
            end.y - ARROW_LEN * sin(angle + ARROW_HALF_ANGLE),
        )
        drawPath(
            path = Path().apply {
                moveTo(end.x, end.y)
                lineTo(a1.x, a1.y)
                lineTo(a2.x, a2.y)
                close()
            },
            color = style.edgeColor,
        )
    }

    if (label.isNotBlank()) {
        val measured = textMeasurer.measure(
            text = label,
            style = TextStyle(fontSize = style.labelFontSize, color = style.labelColor),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        val perpSign = if (labelPerpX >= 0f) 1f else -1f
        val perpDist = if (labelPerpX == 0f && labelPerpY == 0f) 0f else 14f
        val textX = labelPos.x + labelPerpX * perpSign * perpDist - measured.size.width / 2f
        val textY = labelPos.y + labelPerpY * perpSign * perpDist - measured.size.height / 2f
        if (style.labelBgColor != null) {
            val bgPad = 3f
            drawRoundRect(
                color = style.labelBgColor,
                topLeft = Offset(textX - bgPad, textY - bgPad),
                size = Size(measured.size.width + bgPad * 2f, measured.size.height + bgPad * 2f),
                cornerRadius = CornerRadius(4f),
            )
        }
        drawText(textLayoutResult = measured, topLeft = Offset(textX, textY))
    }
}
