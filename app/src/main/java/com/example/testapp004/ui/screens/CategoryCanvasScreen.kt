package com.example.testapp004.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.example.testapp004.viewmodel.CanvasPersonNode
import com.example.testapp004.viewmodel.CanvasRelationEdge
import com.example.testapp004.viewmodel.CategoryCanvasViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val NODE_RADIUS = 50f
private const val ARROW_LEN = 18f
private const val ARROW_HALF_ANGLE = 0.4f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCanvasScreen(
    viewModel: CategoryCanvasViewModel,
    onNavigateBack: () -> Unit,
    onPersonClick: (Long) -> Unit,
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
                )
            }
        }
    }
}

@Composable
private fun CanvasGraph(
    nodes: List<CanvasPersonNode>,
    edges: List<CanvasRelationEdge>,
    onPersonClick: (Long) -> Unit,
) {
    val nodeMap = remember(nodes) { nodes.associateBy { it.id } }

    var zoom by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    LaunchedEffect(nodes, canvasSize) {
        if (nodes.isEmpty() || canvasSize == Size.Zero) return@LaunchedEffect
        val padding = 80f
        val minX = nodes.minOf { it.x } - NODE_RADIUS - padding
        val minY = nodes.minOf { it.y } - NODE_RADIUS - padding
        val maxX = nodes.maxOf { it.x } + NODE_RADIUS + padding
        val maxY = nodes.maxOf { it.y } + NODE_RADIUS + padding
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
    val nodeColor = MaterialTheme.colorScheme.primaryContainer
    val nodeStrokeColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer
    val edgeColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomChange, _ ->
                    val newZoom = (zoom * zoomChange).coerceIn(0.1f, 5f)
                    panOffset = centroid - (centroid - panOffset) * (newZoom / zoom) + pan
                    zoom = newZoom
                }
            }
            .pointerInput(nodes, zoom, panOffset) {
                detectTapGestures { tapOffset ->
                    val vx = (tapOffset.x - panOffset.x) / zoom
                    val vy = (tapOffset.y - panOffset.y) / zoom
                    nodes.firstOrNull { node ->
                        val dx = vx - node.x
                        val dy = vy - node.y
                        dx * dx + dy * dy <= NODE_RADIUS * NODE_RADIUS
                    }?.let { onPersonClick(it.id) }
                }
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
                    label = edge.label,
                    edgeColor = edgeColor,
                    labelColor = labelColor,
                    textMeasurer = textMeasurer,
                )
            }
            nodes.forEach { node ->
                drawNode(
                    center = Offset(node.x, node.y),
                    name = node.name,
                    nodeColor = nodeColor,
                    strokeColor = nodeStrokeColor,
                    textColor = textColor,
                    textMeasurer = textMeasurer,
                )
            }
        }
    }
}

private fun DrawScope.drawEdge(
    from: Offset,
    to: Offset,
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

    val start = Offset(from.x + ux * NODE_RADIUS, from.y + uy * NODE_RADIUS)
    val end = Offset(to.x - ux * NODE_RADIUS, to.y - uy * NODE_RADIUS)

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
    name: String,
    nodeColor: Color,
    strokeColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer,
) {
    drawCircle(color = nodeColor, radius = NODE_RADIUS, center = center)
    drawCircle(color = strokeColor, radius = NODE_RADIUS, center = center, style = Stroke(width = 2.5f))

    val measured = textMeasurer.measure(
        text = name,
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor),
        overflow = TextOverflow.Ellipsis,
        maxLines = 2,
        constraints = Constraints(maxWidth = (NODE_RADIUS * 1.8f).toInt()),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(center.x - measured.size.width / 2f, center.y - measured.size.height / 2f),
    )
}
