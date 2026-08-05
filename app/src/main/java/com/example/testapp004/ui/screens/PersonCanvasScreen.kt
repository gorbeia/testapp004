package com.example.testapp004.ui.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.canvasgraph.EdgeStyle
import com.example.canvasgraph.GraphEdge
import com.example.canvasgraph.GraphNode
import com.example.canvasgraph.LayoutEngineType
import com.example.canvasgraph.NodeStyle
import com.example.canvasgraph.RelationGraph
import com.example.testapp004.model.RelationCategory
import com.example.testapp004.model.RelationTypeOption
import com.example.testapp004.model.RelationTypes
import com.example.testapp004.viewmodel.CanvasPersonNode
import com.example.testapp004.viewmodel.CanvasRelationEdge
import com.example.testapp004.viewmodel.PersonCanvasViewModel

private val CanvasEdgeLabelFontSize = 9.sp

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
            else -> {
                val graphNodes = rememberPersonGraphNodes(
                    nodes = uiState.nodes,
                    edges = uiState.edges,
                    centerId = viewModel.acquaintanceId,
                )
                val graphEdges = rememberPersonGraphEdges(uiState.edges)
                RelationGraph(
                    nodes = graphNodes,
                    edges = graphEdges,
                    onNodeTap = onPersonClick,
                    onRelationDrop = viewModel::openRelationDialog,
                    modifier = Modifier.fillMaxSize(),
                )
            }
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
            layoutEngineType = uiState.layoutEngineType,
            onDistanceChange = viewModel::setRelationDistance,
            onLayoutEngineTypeChange = viewModel::setLayoutEngineType,
            onDismiss = onControlSheetDismiss,
        )
    }
}

@Composable
private fun rememberPersonGraphNodes(
    nodes: List<CanvasPersonNode>,
    edges: List<CanvasRelationEdge>,
    centerId: Long,
): List<GraphNode> {
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

    return remember(nodes, edges, centerId, cs) {
        nodes.map { node ->
            val cat = node.dominantCategory
            val dist = node.distanceFromCategory
            val isCenter = node.id == centerId
            val baseFill = if (node.isNetSource == true) {
                categoryFillSource[cat] ?: defaultFillSource
            } else {
                categoryFill[cat] ?: defaultFill
            }
            val baseStroke = categoryStroke[cat] ?: defaultStroke
            val baseText = if (node.isNetSource == true) {
                categoryTextSource[cat] ?: defaultTextSource
            } else {
                categoryText[cat] ?: defaultText
            }
            // Blend toward surface for nodes reached only through relation distance expansion
            val fill = when {
                isCenter -> baseFill
                dist >= 2 -> lerp(baseFill, cs.surfaceContainerHighest, 0.65f)
                dist == 1 -> lerp(baseFill, cs.surfaceContainerHighest, 0.45f)
                else -> lerp(baseFill, cs.surfaceContainerHighest, 0.25f)
            }
            val stroke = when {
                isCenter -> baseStroke
                dist >= 2 -> lerp(baseStroke, cs.surfaceContainerHighest, 0.55f)
                dist == 1 -> lerp(baseStroke, cs.surfaceContainerHighest, 0.35f)
                else -> lerp(baseStroke, cs.surfaceContainerHighest, 0.20f)
            }
            val text = when {
                isCenter -> baseText
                dist >= 2 -> lerp(baseText, cs.onSurfaceVariant, 0.35f)
                dist == 1 -> lerp(baseText, cs.onSurfaceVariant, 0.20f)
                else -> lerp(baseText, cs.onSurfaceVariant, 0.10f)
            }
            GraphNode(
                id = node.id,
                name = node.name,
                x = node.x,
                y = node.y,
                style = NodeStyle(
                    fillColor = fill,
                    strokeColor = stroke,
                    textColor = text,
                    useDashedBorder = dist > 0,
                    // Outer ring marks the focal person of this canvas view
                    ringColor = if (isCenter) cs.primary else null,
                ),
                isCenterNode = isCenter,
            )
        }
    }
}

@Composable
private fun rememberPersonGraphEdges(edges: List<CanvasRelationEdge>): List<GraphEdge> {
    val cs = MaterialTheme.colorScheme
    val categoryStroke = mapOf(
        RelationCategory.FAMILY to cs.tertiary,
        RelationCategory.PROFESSIONAL to cs.primary,
        RelationCategory.SOCIAL to cs.secondary,
    )
    return remember(edges, cs) {
        edges.map { edge ->
            GraphEdge(
                id = edge.id,
                fromId = edge.fromId,
                toId = edge.toId,
                label = edge.label,
                style = EdgeStyle(
                    edgeColor = categoryStroke[edge.category] ?: cs.outline,
                    labelColor = cs.onSurface,
                    labelBgColor = cs.surface.copy(alpha = 0.82f),
                    labelFontSize = CanvasEdgeLabelFontSize,
                ),
                isSymmetric = edge.isSymmetric,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonCanvasControlSheet(
    relationDistance: Int,
    layoutEngineType: LayoutEngineType,
    onDistanceChange: (Int) -> Unit,
    onLayoutEngineTypeChange: (LayoutEngineType) -> Unit,
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
            Text("Layout", style = MaterialTheme.typography.titleSmall)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LayoutEngineType.values().forEach { type ->
                    FilterChip(
                        selected = layoutEngineType == type,
                        onClick = { onLayoutEngineTypeChange(type) },
                        label = { Text(type.displayName) },
                    )
                }
            }
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
