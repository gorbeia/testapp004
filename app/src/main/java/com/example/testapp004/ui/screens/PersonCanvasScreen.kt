package com.example.testapp004.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.testapp004.model.RelationCategory
import com.example.testapp004.model.RelationTypeOption
import com.example.testapp004.model.RelationTypes
import com.example.testapp004.viewmodel.CanvasPersonNode
import com.example.testapp004.viewmodel.CanvasRelationEdge
import com.example.testapp004.viewmodel.PersonCanvasViewModel
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val PC_NODE_HALF_H = 26f
private const val PC_NODE_MAX_HALF_W = 110f
private const val PC_NODE_H_PAD = 18f
private const val PC_ARROW_LEN = 18f
private const val PC_ARROW_HALF_ANGLE = 0.4f
private const val PC_LAYER_H = 170f

private data class PcEdgeLabelKey(val label: String, val fromRow: Int, val toRow: Int)

@Composable
internal fun PersonCanvasContent(
    viewModel: PersonCanvasViewModel,
    paddingValues: PaddingValues,
    onPersonClick: (Long) -> Unit,
    isControlSheetOpen: Boolean,
    onControlSheetDismiss: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.nodes.isEmpty() -> Text(
                text = "No relations yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> PersonCanvasGraph(
                nodes = uiState.nodes,
                edges = uiState.edges,
                centerId = viewModel.acquaintanceId,
                onPersonClick = onPersonClick,
                onRelationDrop = viewModel::openRelationDialog,
            )
        }
    }

    if (uiState.isRelationDialogOpen &&
        uiState.pendingRelationFromId != null &&
        uiState.pendingRelationToId != null
    ) {
        PersonCanvasAddRelationDialog(
            fromPersonName = uiState.pendingRelationFromName,
            toPersonName = uiState.pendingRelationToName,
            onConfirm = { typeKey, isDragSourceFrom, customLabel ->
                viewModel.addRelationFromCanvas(typeKey, isDragSourceFrom, customLabel)
            },
            onDismiss = viewModel::closeRelationDialog,
        )
    }

    if (isControlSheetOpen) {
        PersonCanvasControlSheet(
            relationDistance = uiState.relationDistance,
            onDistanceChange = viewModel::setRelationDistance,
            onDismiss = onControlSheetDismiss,
        )
    }
}

@Composable
private fun PersonCanvasGraph(
    nodes: List<CanvasPersonNode>,
    edges: List<CanvasRelationEdge>,
    centerId: Long,
    onPersonClick: (Long) -> Unit,
    onRelationDrop: (fromId: Long, toId: Long) -> Unit,
) {
    val nodeMap = remember(nodes) { nodes.associateBy { it.id } }

    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    var isDragging by remember { mutableStateOf(false) }
    var draggedNodeId by remember { mutableStateOf<Long?>(null) }
    var dragScreenPos by remember { mutableStateOf(Offset.Zero) }
    var dropTargetId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(nodes, canvasSize) {
        if (nodes.isEmpty() || canvasSize == Size.Zero) return@LaunchedEffect
        val padding = 80f
        val minX = nodes.minOf { it.x } - PC_NODE_MAX_HALF_W - padding
        val minY = nodes.minOf { it.y } - PC_NODE_HALF_H - padding
        val maxX = nodes.maxOf { it.x } + PC_NODE_MAX_HALF_W + padding
        val maxY = nodes.maxOf { it.y } + PC_NODE_HALF_H + padding
        val contentW = maxX - minX
        val contentH = maxY - minY
        zoom = minOf(canvasSize.width / contentW, canvasSize.height / contentH, 1.2f)
            .coerceAtLeast(0.1f)
        panOffset = Offset(
            (canvasSize.width - contentW * zoom) / 2f - minX * zoom,
            (canvasSize.height - contentH * zoom) / 2f - minY * zoom,
        )
    }

    val textMeasurer = rememberTextMeasurer()
    val nodeHalfWidths = remember(nodes, textMeasurer) {
        nodes.associate { node ->
            val m = textMeasurer.measure(
                text = node.name,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                constraints = Constraints(
                    maxWidth = ((PC_NODE_MAX_HALF_W - PC_NODE_H_PAD) * 2).toInt(),
                ),
            )
            node.id to (m.size.width / 2f + PC_NODE_H_PAD).coerceIn(PC_NODE_HALF_H, PC_NODE_MAX_HALF_W)
        }
    }

    val cs = MaterialTheme.colorScheme
    val categoryFill = mapOf(
        RelationCategory.FAMILY to lerp(cs.tertiaryContainer, cs.tertiary, 0.25f),
        RelationCategory.PROFESSIONAL to lerp(cs.primaryContainer, cs.primary, 0.25f),
        RelationCategory.SOCIAL to lerp(cs.secondaryContainer, cs.secondary, 0.25f),
    )
    val categoryFillSource = mapOf(
        RelationCategory.FAMILY to cs.tertiary,
        RelationCategory.PROFESSIONAL to cs.primary,
        RelationCategory.SOCIAL to cs.secondary,
    )
    val categoryStroke = mapOf(
        RelationCategory.FAMILY to cs.tertiary,
        RelationCategory.PROFESSIONAL to cs.primary,
        RelationCategory.SOCIAL to cs.secondary,
    )
    val categoryText = mapOf(
        RelationCategory.FAMILY to cs.onTertiaryContainer,
        RelationCategory.PROFESSIONAL to cs.onPrimaryContainer,
        RelationCategory.SOCIAL to cs.onSecondaryContainer,
    )
    val categoryTextSource = mapOf(
        RelationCategory.FAMILY to cs.onTertiary,
        RelationCategory.PROFESSIONAL to cs.onPrimary,
        RelationCategory.SOCIAL to cs.onSecondary,
    )
    val defaultFill = lerp(cs.primaryContainer, cs.primary, 0.25f)
    val defaultFillSource = cs.primary
    val defaultStroke = cs.primary
    val defaultText = cs.onPrimaryContainer
    val defaultTextSource = cs.onPrimary
    val labelColor = cs.onSurface
    val labelBgColor = cs.surface.copy(alpha = 0.82f)
    val dropTargetHighlightColor = cs.tertiary
    val centerHighlightColor = cs.primary

    fun nodeFill(cat: RelationCategory?, isCenter: Boolean, isNetSource: Boolean?, distance: Int): Color {
        val base = if (isNetSource == true) {
            categoryFillSource[cat] ?: defaultFillSource
        } else {
            categoryFill[cat] ?: defaultFill
        }
        return when {
            isCenter -> base
            distance >= 2 -> lerp(base, cs.surfaceContainerHighest, 0.65f)
            distance == 1 -> lerp(base, cs.surfaceContainerHighest, 0.45f)
            else -> lerp(base, cs.surfaceContainerHighest, 0.25f)
        }
    }

    fun nodeStroke(cat: RelationCategory?, isCenter: Boolean, distance: Int): Color {
        val base = categoryStroke[cat] ?: defaultStroke
        return when {
            isCenter -> base
            distance >= 2 -> lerp(base, cs.surfaceContainerHighest, 0.55f)
            distance == 1 -> lerp(base, cs.surfaceContainerHighest, 0.35f)
            else -> lerp(base, cs.surfaceContainerHighest, 0.20f)
        }
    }

    fun nodeText(cat: RelationCategory?, isCenter: Boolean, isNetSource: Boolean?, distance: Int): Color {
        val base = if (isNetSource == true) {
            categoryTextSource[cat] ?: defaultTextSource
        } else {
            categoryText[cat] ?: defaultText
        }
        return when {
            isCenter -> base
            distance >= 2 -> lerp(base, cs.onSurfaceVariant, 0.35f)
            distance == 1 -> lerp(base, cs.onSurfaceVariant, 0.20f)
            else -> lerp(base, cs.onSurfaceVariant, 0.10f)
        }
    }

    fun edgeColor(cat: RelationCategory?) = categoryStroke[cat] ?: cs.outline

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomChange, _ ->
                    if (!isDragging) {
                        val newZoom = (zoom * zoomChange).coerceIn(0.1f, 5f)
                        panOffset = centroid - (centroid - panOffset) * (newZoom / zoom) + pan
                        zoom = newZoom
                    }
                }
            }
            .pointerInput(nodes, zoom, panOffset) {
                detectTapGestures { tapOffset ->
                    val vx = (tapOffset.x - panOffset.x) / zoom
                    val vy = (tapOffset.y - panOffset.y) / zoom
                    nodes.firstOrNull { node ->
                        val dx = vx - node.x
                        val dy = vy - node.y
                        abs(dx) <= (nodeHalfWidths[node.id] ?: PC_NODE_MAX_HALF_W) &&
                            abs(dy) <= PC_NODE_HALF_H
                    }?.let { onPersonClick(it.id) }
                }
            }
            .pointerInput(nodes, zoom, panOffset) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val vx = (offset.x - panOffset.x) / zoom
                        val vy = (offset.y - panOffset.y) / zoom
                        val node = nodes.firstOrNull { n ->
                            abs(vx - n.x) <= (nodeHalfWidths[n.id] ?: PC_NODE_MAX_HALF_W) &&
                                abs(vy - n.y) <= PC_NODE_HALF_H
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
                            val vx = (change.position.x - panOffset.x) / zoom
                            val vy = (change.position.y - panOffset.y) / zoom
                            dropTargetId = nodes.firstOrNull { n ->
                                n.id != draggedNodeId &&
                                    abs(vx - n.x) <= (nodeHalfWidths[n.id] ?: PC_NODE_MAX_HALF_W) &&
                                    abs(vy - n.y) <= PC_NODE_HALF_H
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
            translate(panOffset.x, panOffset.y)
            scale(zoom, zoom, Offset.Zero)
        }) {
            edges.forEach { edge ->
                val from = nodeMap[edge.fromId] ?: return@forEach
                val to = nodeMap[edge.toId] ?: return@forEach
                pcDrawEdge(
                    from = Offset(from.x, from.y),
                    to = Offset(to.x, to.y),
                    fromHalfW = nodeHalfWidths[edge.fromId] ?: PC_NODE_MAX_HALF_W,
                    toHalfW = nodeHalfWidths[edge.toId] ?: PC_NODE_MAX_HALF_W,
                    edgeColor = edgeColor(edge.category),
                    drawArrow = !edge.isSymmetric,
                )
            }
            pcDrawEdgeLabels(
                edges = edges,
                nodeMap = nodeMap,
                labelColor = labelColor,
                labelBgColor = labelBgColor,
                textMeasurer = textMeasurer,
            )
            nodes.forEach { node ->
                val isCenter = node.id == centerId
                val isDropTarget = isDragging && node.id == dropTargetId
                val isDragSource = isDragging && node.id == draggedNodeId
                val dist = node.distanceFromCategory
                val fill = nodeFill(node.dominantCategory, isCenter, node.isNetSource, dist)
                val stroke = nodeStroke(node.dominantCategory, isCenter, dist)
                val text = nodeText(node.dominantCategory, isCenter, node.isNetSource, dist)

                if (isCenter) {
                    val hw = nodeHalfWidths[node.id] ?: PC_NODE_MAX_HALF_W
                    val pad = 5f
                    drawRoundRect(
                        color = centerHighlightColor.copy(alpha = 0.35f),
                        topLeft = Offset(node.x - hw - pad, node.y - PC_NODE_HALF_H - pad),
                        size = Size((hw + pad) * 2, (PC_NODE_HALF_H + pad) * 2),
                        cornerRadius = CornerRadius(PC_NODE_HALF_H + pad),
                        style = Stroke(width = 4f),
                    )
                }

                pcDrawNode(
                    center = Offset(node.x, node.y),
                    halfW = nodeHalfWidths[node.id] ?: PC_NODE_MAX_HALF_W,
                    name = node.name,
                    nodeColor = if (isDragSource) fill.copy(alpha = 0.3f) else fill,
                    strokeColor = when {
                        isDropTarget -> dropTargetHighlightColor
                        isDragSource -> stroke.copy(alpha = 0.3f)
                        else -> stroke
                    },
                    textColor = if (isDragSource) text.copy(alpha = 0.3f) else text,
                    textMeasurer = textMeasurer,
                    useDashedBorder = dist > 0,
                )

                if (isDropTarget) {
                    val hw = nodeHalfWidths[node.id] ?: PC_NODE_MAX_HALF_W
                    val pad = 6f
                    drawRoundRect(
                        color = dropTargetHighlightColor,
                        topLeft = Offset(node.x - hw - pad, node.y - PC_NODE_HALF_H - pad),
                        size = Size((hw + pad) * 2, (PC_NODE_HALF_H + pad) * 2),
                        cornerRadius = CornerRadius(PC_NODE_HALF_H + pad),
                        style = Stroke(width = 3.5f),
                    )
                }
            }
            if (isDragging) {
                val id = draggedNodeId
                val ghostNode = if (id != null) nodeMap[id] else null
                if (ghostNode != null) {
                    val isCenter = ghostNode.id == centerId
                    val ghostDist = ghostNode.distanceFromCategory
                    val ghostCanvasX = (dragScreenPos.x - panOffset.x) / zoom
                    val ghostCanvasY = (dragScreenPos.y - panOffset.y) / zoom
                    pcDrawNode(
                        center = Offset(ghostCanvasX, ghostCanvasY),
                        halfW = nodeHalfWidths[ghostNode.id] ?: PC_NODE_MAX_HALF_W,
                        name = ghostNode.name,
                        nodeColor = nodeFill(ghostNode.dominantCategory, isCenter, ghostNode.isNetSource, ghostDist),
                        strokeColor = if (dropTargetId != null) {
                            dropTargetHighlightColor
                        } else {
                            nodeStroke(ghostNode.dominantCategory, isCenter, ghostDist)
                        },
                        textColor = nodeText(
                            ghostNode.dominantCategory,
                            isCenter,
                            ghostNode.isNetSource,
                            ghostDist,
                        ),
                        textMeasurer = textMeasurer,
                    )
                }
            }
        }
    }
}

private fun pcRectBorderPoint(cx: Float, cy: Float, ux: Float, uy: Float, hw: Float, hh: Float): Offset {
    val ax = abs(ux)
    val ay = abs(uy)
    val t = if (ax * hh >= ay * hw) hw / ax else hh / ay
    return Offset(cx + ux * t, cy + uy * t)
}

private fun DrawScope.pcDrawEdge(
    from: Offset,
    to: Offset,
    fromHalfW: Float,
    toHalfW: Float,
    edgeColor: Color,
    drawArrow: Boolean = true,
) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 1f) return

    val ux = dx / length
    val uy = dy / length

    val start = pcRectBorderPoint(from.x, from.y, ux, uy, fromHalfW, PC_NODE_HALF_H)
    val end = pcRectBorderPoint(to.x, to.y, -ux, -uy, toHalfW, PC_NODE_HALF_H)

    drawLine(color = edgeColor, start = start, end = end, strokeWidth = 2.5f, cap = StrokeCap.Round)

    if (drawArrow) {
        val angle = atan2(uy, ux)
        val a1 = Offset(
            end.x - PC_ARROW_LEN * cos(angle - PC_ARROW_HALF_ANGLE),
            end.y - PC_ARROW_LEN * sin(angle - PC_ARROW_HALF_ANGLE),
        )
        val a2 = Offset(
            end.x - PC_ARROW_LEN * cos(angle + PC_ARROW_HALF_ANGLE),
            end.y - PC_ARROW_LEN * sin(angle + PC_ARROW_HALF_ANGLE),
        )
        drawPath(
            path = Path().apply {
                moveTo(end.x, end.y)
                lineTo(a1.x, a1.y)
                lineTo(a2.x, a2.y)
                close()
            },
            color = edgeColor,
        )
    }
}

private fun DrawScope.pcDrawEdgeLabels(
    edges: List<CanvasRelationEdge>,
    nodeMap: Map<Long, CanvasPersonNode>,
    labelColor: Color,
    labelBgColor: Color,
    textMeasurer: TextMeasurer,
) {
    val groups = mutableMapOf<PcEdgeLabelKey, MutableList<Pair<Float, Float>>>()
    edges.forEach { edge ->
        if (edge.label.isBlank()) return@forEach
        val from = nodeMap[edge.fromId] ?: return@forEach
        val to = nodeMap[edge.toId] ?: return@forEach
        val key = PcEdgeLabelKey(
            label = edge.label,
            fromRow = (from.y / PC_LAYER_H).roundToInt(),
            toRow = (to.y / PC_LAYER_H).roundToInt(),
        )
        groups.getOrPut(key) { mutableListOf() }.add((from.x + to.x) / 2f to (from.y + to.y) / 2f)
    }
    groups.forEach { (key, mids) ->
        val cx = mids.sumOf { it.first.toDouble() }.toFloat() / mids.size
        val cy = mids.sumOf { it.second.toDouble() }.toFloat() / mids.size
        val measured = textMeasurer.measure(
            text = key.label,
            style = TextStyle(fontSize = 11.sp, color = labelColor),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        val textX = cx - measured.size.width / 2f
        val textY = cy - measured.size.height / 2f
        val bgPad = 3f
        drawRoundRect(
            color = labelBgColor,
            topLeft = Offset(textX - bgPad, textY - bgPad),
            size = Size(measured.size.width + bgPad * 2f, measured.size.height + bgPad * 2f),
            cornerRadius = CornerRadius(4f),
        )
        drawText(textLayoutResult = measured, topLeft = Offset(textX, textY))
    }
}

private fun DrawScope.pcDrawNode(
    center: Offset,
    halfW: Float,
    name: String,
    nodeColor: Color,
    strokeColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer,
    useDashedBorder: Boolean = false,
) {
    val topLeft = Offset(center.x - halfW, center.y - PC_NODE_HALF_H)
    val size = Size(halfW * 2, PC_NODE_HALF_H * 2)
    val corner = CornerRadius(PC_NODE_HALF_H, PC_NODE_HALF_H)
    drawRoundRect(color = nodeColor, topLeft = topLeft, size = size, cornerRadius = corner)
    drawRoundRect(
        color = strokeColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = corner,
        style = Stroke(
            width = 2.5f,
            pathEffect = if (useDashedBorder) PathEffect.dashPathEffect(floatArrayOf(8f, 4f)) else null,
        ),
    )
    val measured = textMeasurer.measure(
        text = name,
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = ((halfW - PC_NODE_H_PAD) * 2).coerceAtLeast(1f).toInt()),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(center.x - measured.size.width / 2f, center.y - measured.size.height / 2f),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonCanvasControlSheet(
    relationDistance: Int,
    onDistanceChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Distance", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                (0..2).forEach { d ->
                    FilterChip(
                        selected = relationDistance == d,
                        onClick = { onDistanceChange(d) },
                        label = { Text(d.toString()) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonCanvasAddRelationDialog(
    fromPersonName: String,
    toPersonName: String,
    onConfirm: (typeKey: String, isDragSourceFrom: Boolean, customLabel: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val typeOptions = remember { RelationTypes.typeOptions() }

    var selectedOption by remember { mutableStateOf<RelationTypeOption?>(null) }
    var customLabel by remember { mutableStateOf("") }
    var isTypeDropdownExpanded by remember { mutableStateOf(false) }

    val isCustom = selectedOption?.typeKey == RelationTypes.CUSTOM_KEY
    val isConfirmEnabled = selectedOption != null && (!isCustom || customLabel.isNotBlank())
    val counterpartLabel: String? = selectedOption?.takeIf { !isCustom }?.let { opt ->
        RelationTypes.findByKey(opt.typeKey)?.let { type ->
            if (opt.isCurrentPersonFrom) type.toLabel else type.fromLabel
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Relation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = fromPersonName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = toPersonName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                ExposedDropdownMenuBox(
                    expanded = isTypeDropdownExpanded,
                    onExpandedChange = { isTypeDropdownExpanded = !isTypeDropdownExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedOption?.displayLabel ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("$fromPersonName's role") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = isTypeDropdownExpanded,
                        onDismissRequest = { isTypeDropdownExpanded = false },
                    ) {
                        var lastCategory: RelationCategory? = null
                        typeOptions.forEach { option ->
                            if (option.typeKey != RelationTypes.CUSTOM_KEY && option.category != lastCategory) {
                                lastCategory = option.category
                                val categoryLabel = when (option.category) {
                                    RelationCategory.FAMILY -> "Family"
                                    RelationCategory.PROFESSIONAL -> "Professional"
                                    RelationCategory.SOCIAL -> "Social"
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = categoryLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    },
                                    onClick = {},
                                    enabled = false,
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(option.displayLabel) },
                                onClick = {
                                    selectedOption = option
                                    customLabel = ""
                                    isTypeDropdownExpanded = false
                                },
                            )
                        }
                    }
                }
                if (counterpartLabel != null) {
                    Text(
                        text = "→ $toPersonName will appear as: $counterpartLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (isCustom) {
                    OutlinedTextField(
                        value = customLabel,
                        onValueChange = { customLabel = it },
                        label = { Text("Label") },
                        placeholder = { Text("e.g. mentor, colleague") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val opt = selectedOption ?: return@TextButton
                    onConfirm(opt.typeKey, opt.isCurrentPersonFrom, customLabel.takeIf { isCustom })
                },
                enabled = isConfirmEnabled,
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
