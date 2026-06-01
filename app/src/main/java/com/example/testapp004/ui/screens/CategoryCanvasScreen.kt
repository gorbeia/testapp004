package com.example.testapp004.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.example.testapp004.viewmodel.CategoryCanvasViewModel
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val NODE_HALF_H = 26f
private const val NODE_MAX_HALF_W = 110f
private const val NODE_H_PAD = 18f
private const val ARROW_LEN = 18f
private const val ARROW_HALF_ANGLE = 0.4f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCanvasScreen(
    viewModel: CategoryCanvasViewModel,
    onNavigateBack: () -> Unit,
    onPersonClick: (Long) -> Unit,
    onAddPersonClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.categoryName.isNotEmpty()) uiState.categoryName else "Canvas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPersonClick) {
                Icon(Icons.Default.Add, contentDescription = "Add person")
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.nodes.isEmpty() -> Text(
                    text = "No people in this category",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> CanvasGraph(
                    nodes = uiState.nodes,
                    edges = uiState.edges,
                    onPersonClick = onPersonClick,
                    onRelationDrop = viewModel::openRelationDialog,
                )
            }
        }
    }

    if (uiState.isRelationDialogOpen &&
        uiState.pendingRelationFromId != null &&
        uiState.pendingRelationToId != null
    ) {
        CanvasAddRelationDialog(
            fromPersonName = uiState.pendingRelationFromName,
            toPersonName = uiState.pendingRelationToName,
            onConfirm = { typeKey, isDragSourceFrom, customLabel ->
                viewModel.addRelationFromCanvas(typeKey, isDragSourceFrom, customLabel)
            },
            onDismiss = viewModel::closeRelationDialog,
        )
    }
}

@Composable
private fun CanvasGraph(
    nodes: List<CanvasPersonNode>,
    edges: List<CanvasRelationEdge>,
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
        val minX = nodes.minOf { it.x } - NODE_MAX_HALF_W - padding
        val minY = nodes.minOf { it.y } - NODE_HALF_H - padding
        val maxX = nodes.maxOf { it.x } + NODE_MAX_HALF_W + padding
        val maxY = nodes.maxOf { it.y } + NODE_HALF_H + padding
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
                constraints = Constraints(maxWidth = ((NODE_MAX_HALF_W - NODE_H_PAD) * 2).toInt()),
            )
            node.id to (m.size.width / 2f + NODE_H_PAD).coerceIn(NODE_HALF_H, NODE_MAX_HALF_W)
        }
    }
    val cs = MaterialTheme.colorScheme
    val categoryFill = mapOf(
        RelationCategory.FAMILY to cs.tertiaryContainer,
        RelationCategory.PROFESSIONAL to cs.primaryContainer,
        RelationCategory.SOCIAL to cs.secondaryContainer,
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
    val defaultFill = cs.primaryContainer
    val defaultStroke = cs.primary
    val defaultText = cs.onPrimaryContainer
    val labelColor = cs.onSurface
    val dropTargetHighlightColor = cs.tertiary

    fun nodeFill(cat: RelationCategory?, direct: Boolean): Color {
        val base = categoryFill[cat] ?: defaultFill
        return if (direct) base else lerp(base, cs.surface, 0.45f)
    }

    fun nodeStroke(cat: RelationCategory?, direct: Boolean): Color {
        val base = categoryStroke[cat] ?: defaultStroke
        return if (direct) base else lerp(base, cs.surface, 0.45f)
    }

    fun nodeText(cat: RelationCategory?) = categoryText[cat] ?: defaultText

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
                        abs(dx) <= (nodeHalfWidths[node.id] ?: NODE_MAX_HALF_W) &&
                            abs(dy) <= NODE_HALF_H
                    }?.let { onPersonClick(it.id) }
                }
            }
            .pointerInput(nodes, zoom, panOffset) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val vx = (offset.x - panOffset.x) / zoom
                        val vy = (offset.y - panOffset.y) / zoom
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
                            val vx = (change.position.x - panOffset.x) / zoom
                            val vy = (change.position.y - panOffset.y) / zoom
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
                            if (from != null && to != null) {
                                onRelationDrop(from, to)
                            }
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
                drawEdge(
                    from = Offset(from.x, from.y),
                    to = Offset(to.x, to.y),
                    fromHalfW = nodeHalfWidths[edge.fromId] ?: NODE_MAX_HALF_W,
                    toHalfW = nodeHalfWidths[edge.toId] ?: NODE_MAX_HALF_W,
                    label = edge.label,
                    edgeColor = edgeColor(edge.category),
                    labelColor = labelColor,
                    textMeasurer = textMeasurer,
                )
            }
            nodes.forEach { node ->
                val isDropTarget = isDragging && node.id == dropTargetId
                val isDragSource = isDragging && node.id == draggedNodeId
                val fill = nodeFill(node.dominantCategory, node.isDirectMember)
                val stroke = nodeStroke(node.dominantCategory, node.isDirectMember)
                val text = nodeText(node.dominantCategory)
                drawNode(
                    center = Offset(node.x, node.y),
                    halfW = nodeHalfWidths[node.id] ?: NODE_MAX_HALF_W,
                    name = node.name,
                    nodeColor = if (isDragSource) fill.copy(alpha = 0.3f) else fill,
                    strokeColor = when {
                        isDropTarget -> dropTargetHighlightColor
                        isDragSource -> stroke.copy(alpha = 0.3f)
                        else -> stroke
                    },
                    textColor = if (isDragSource) text.copy(alpha = 0.3f) else text,
                    textMeasurer = textMeasurer,
                )
                if (isDropTarget) {
                    val hw = nodeHalfWidths[node.id] ?: NODE_MAX_HALF_W
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
                val id = draggedNodeId
                val ghostNode = if (id != null) nodeMap[id] else null
                if (ghostNode != null) {
                    val ghostCanvasX = (dragScreenPos.x - panOffset.x) / zoom
                    val ghostCanvasY = (dragScreenPos.y - panOffset.y) / zoom
                    drawNode(
                        center = Offset(ghostCanvasX, ghostCanvasY),
                        halfW = nodeHalfWidths[ghostNode.id] ?: NODE_MAX_HALF_W,
                        name = ghostNode.name,
                        nodeColor = nodeFill(ghostNode.dominantCategory, ghostNode.isDirectMember),
                        strokeColor = if (dropTargetId != null) {
                            dropTargetHighlightColor
                        } else {
                            nodeStroke(ghostNode.dominantCategory, ghostNode.isDirectMember)
                        },
                        textColor = nodeText(ghostNode.dominantCategory),
                        textMeasurer = textMeasurer,
                    )
                }
            }
        }
    }
}

private fun rectBorderPoint(cx: Float, cy: Float, ux: Float, uy: Float, hw: Float, hh: Float): Offset {
    val ax = abs(ux)
    val ay = abs(uy)
    val t = if (ax * hh >= ay * hw) hw / ax else hh / ay
    return Offset(cx + ux * t, cy + uy * t)
}

private fun DrawScope.drawEdge(
    from: Offset,
    to: Offset,
    fromHalfW: Float,
    toHalfW: Float,
    label: String,
    edgeColor: Color,
    labelColor: Color,
    textMeasurer: TextMeasurer,
) {
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 1f) return

    val ux = dx / length
    val uy = dy / length

    val start = rectBorderPoint(from.x, from.y, ux, uy, fromHalfW, NODE_HALF_H)
    val end = rectBorderPoint(to.x, to.y, -ux, -uy, toHalfW, NODE_HALF_H)

    drawLine(color = edgeColor, start = start, end = end, strokeWidth = 2.5f, cap = StrokeCap.Round)

    val angle = atan2(uy, ux)
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
        color = edgeColor,
    )

    if (label.isNotBlank()) {
        val mid = Offset((start.x + end.x) / 2f, (start.y + end.y) / 2f)
        val measured = textMeasurer.measure(
            text = label,
            style = TextStyle(fontSize = 11.sp, color = labelColor),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(mid.x - measured.size.width / 2f, mid.y - measured.size.height - 4f),
        )
    }
}

private fun DrawScope.drawNode(
    center: Offset,
    halfW: Float,
    name: String,
    nodeColor: Color,
    strokeColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer,
) {
    val topLeft = Offset(center.x - halfW, center.y - NODE_HALF_H)
    val size = Size(halfW * 2, NODE_HALF_H * 2)
    val corner = CornerRadius(NODE_HALF_H, NODE_HALF_H)
    drawRoundRect(color = nodeColor, topLeft = topLeft, size = size, cornerRadius = corner)
    drawRoundRect(
        color = strokeColor,
        topLeft = topLeft,
        size = size,
        cornerRadius = corner,
        style = Stroke(width = 2.5f),
    )

    val measured = textMeasurer.measure(
        text = name,
        style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        constraints = Constraints(maxWidth = ((halfW - NODE_H_PAD) * 2).coerceAtLeast(1f).toInt()),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(center.x - measured.size.width / 2f, center.y - measured.size.height / 2f),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanvasAddRelationDialog(
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
                        label = { Text("Relation type") },
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
