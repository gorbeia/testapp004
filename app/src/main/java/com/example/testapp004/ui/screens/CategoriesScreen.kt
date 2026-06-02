package com.example.testapp004.ui.screens

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.testapp004.model.Category
import com.example.testapp004.viewmodel.CategoriesViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
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
            FloatingActionButton(onClick = viewModel::openAddDialog) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        },
    ) { paddingValues ->
        if (uiState.categories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No categories yet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val treeOrder = buildCategoryTree(uiState.categories)
            val listState = rememberLazyListState()
            var draggingId by remember { mutableStateOf<Long?>(null) }
            var dragOffset by remember { mutableStateOf(0f) }
            var dragXOffset by remember { mutableStateOf(0f) }
            var dropTargetId by remember { mutableStateOf<Long?>(null) }

            val density = LocalDensity.current
            val indentThresholdPx = with(density) { 72.dp.toPx() }

            // Compute proposed parent from X offset for visual feedback during drag.
            val proposedParentId: Long? = run {
                val dragging = draggingId ?: return@run null
                val draggedCat = treeOrder.find { it.first.id == dragging }?.first ?: return@run null
                val candidate = dropTargetId?.let { id -> treeOrder.find { it.first.id == id }?.first }
                    ?: run {
                        val idx = treeOrder.indexOfFirst { it.first.id == dragging }
                        if (idx > 0) treeOrder[idx - 1].first else null
                    }
                when {
                    dragXOffset > indentThresholdPx -> {
                        if (candidate != null && candidate.id != dragging) {
                            val forbidden = descendantIds(uiState.categories, dragging) + dragging
                            if (candidate.id !in forbidden) candidate.id else draggedCat.parentId
                        } else {
                            draggedCat.parentId
                        }
                    }
                    dragXOffset < -indentThresholdPx -> draggedCat.parentId?.let { pid ->
                        treeOrder.find { it.first.id == pid }?.first?.parentId
                    }
                    else -> draggedCat.parentId
                }
            }

            val proposedDepth: Int = if (draggingId == null) {
                0
            } else {
                (proposedParentId?.let { pid -> treeOrder.find { it.first.id == pid }?.second } ?: -1) + 1
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(treeOrder, key = { _, (cat, _) -> cat.id }) { index, (category, depth) ->
                    val isDragging = draggingId == category.id
                    val isDropTarget = dropTargetId == category.id && !isDragging
                    val isProposedParent = proposedParentId == category.id && !isDragging
                    val displayDepth = if (isDragging) proposedDepth else depth
                    CategoryItem(
                        category = category,
                        depth = displayDepth,
                        isDragging = isDragging,
                        isDropTarget = isDropTarget,
                        isProposedParent = isProposedParent,
                        dragOffsetPx = if (isDragging) dragOffset else 0f,
                        onAddChild = { viewModel.openAddChildDialog(category) },
                        onEdit = { viewModel.openEditDialog(category) },
                        onDelete = { viewModel.deleteCategory(category.id) },
                        onDragStart = {
                            draggingId = category.id
                            dragOffset = 0f
                            dragXOffset = 0f
                            dropTargetId = null
                        },
                        onDrag = { dy, dx ->
                            dragOffset += dy
                            dragXOffset += dx
                            val draggedInfo = listState.layoutInfo.visibleItemsInfo
                                .find { it.index == index }
                            if (draggedInfo != null) {
                                val dragCenter = draggedInfo.offset + draggedInfo.size / 2 +
                                    dragOffset.roundToInt()
                                dropTargetId = listState.layoutInfo.visibleItemsInfo
                                    .firstOrNull { info ->
                                        info.index != index &&
                                            dragCenter >= info.offset &&
                                            dragCenter < info.offset + info.size
                                    }
                                    ?.let { info -> treeOrder.getOrNull(info.index)?.first?.id }
                            }
                        },
                        onDragEnd = {
                            val from = draggingId
                            val to = dropTargetId
                            val draggedCat = treeOrder.find { it.first.id == from }?.first
                            val candidate = to?.let { id ->
                                treeOrder.find { it.first.id == id }?.first
                            } ?: run {
                                val idx = treeOrder.indexOfFirst { it.first.id == from }
                                if (idx > 0) treeOrder[idx - 1].first else null
                            }
                            val effectiveParentId: Long? = when {
                                from == null || draggedCat == null -> null
                                dragXOffset > indentThresholdPx -> {
                                    if (candidate != null && candidate.id != from) {
                                        val forbidden = descendantIds(
                                            treeOrder.map { it.first },
                                            from,
                                        ) + from
                                        if (candidate.id !in forbidden) candidate.id else draggedCat.parentId
                                    } else {
                                        draggedCat.parentId
                                    }
                                }
                                dragXOffset < -indentThresholdPx -> draggedCat.parentId?.let { pid ->
                                    treeOrder.find { it.first.id == pid }?.first?.parentId
                                }
                                else -> draggedCat.parentId
                            }
                            draggingId = null
                            dragOffset = 0f
                            dragXOffset = 0f
                            dropTargetId = null
                            if (from != null && draggedCat != null) {
                                if (effectiveParentId != draggedCat.parentId) {
                                    val targetPos = to?.let { toId ->
                                        treeOrder.find { it.first.id == toId }?.first
                                            ?.takeIf { it.parentId == effectiveParentId }?.id
                                    }
                                    viewModel.moveCategory(from, effectiveParentId, targetPos)
                                } else if (to != null) {
                                    val toCat = treeOrder.find { it.first.id == to }?.first
                                    if (toCat?.parentId == draggedCat.parentId) {
                                        viewModel.reorderCategory(from, to)
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            draggingId = null
                            dragOffset = 0f
                            dragXOffset = 0f
                            dropTargetId = null
                        },
                    )
                }
            }
        }
    }

    if (uiState.isAddDialogOpen) {
        AddCategoryDialog(
            existingCategories = uiState.categories,
            preselectedParentId = null,
            onConfirm = { name, parentId -> viewModel.addCategory(name, parentId) },
            onDismiss = viewModel::closeAddDialog,
        )
    }

    uiState.addChildToCategory?.let { parent ->
        AddCategoryDialog(
            existingCategories = uiState.categories,
            preselectedParentId = parent.id,
            onConfirm = { name, parentId -> viewModel.addCategory(name, parentId) },
            onDismiss = viewModel::closeAddChildDialog,
        )
    }

    uiState.editingCategory?.let { editing ->
        EditCategoryDialog(
            category = editing,
            allCategories = uiState.categories,
            onConfirm = { name, parentId -> viewModel.updateCategory(editing.id, name, parentId) },
            onDismiss = viewModel::closeEditDialog,
        )
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    depth: Int,
    isDragging: Boolean,
    isDropTarget: Boolean,
    isProposedParent: Boolean,
    dragOffsetPx: Float,
    onAddChild: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (dy: Float, dx: Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 24).dp)
            .zIndex(if (isDragging) 1f else 0f)
            .offset { IntOffset(0, dragOffsetPx.roundToInt()) }
            .alpha(if (isDragging) 0.85f else 1f)
            .pointerInput(category.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y, dragAmount.x)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() },
                )
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp,
        ),
        colors = when {
            isProposedParent -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            )
            isDropTarget -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
            else -> CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            if (depth > 0) {
                Text(
                    text = "└ ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAddChild) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add child category",
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit category",
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete category",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategoryDialog(
    existingCategories: List<Category>,
    preselectedParentId: Long?,
    onConfirm: (name: String, parentId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedParentId by remember { mutableStateOf(preselectedParentId) }
    var isParentDropdownExpanded by remember { mutableStateOf(false) }
    val selectedParentName = existingCategories.find { it.id == selectedParentId }?.name ?: "None"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existingCategories.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = isParentDropdownExpanded,
                        onExpandedChange = { isParentDropdownExpanded = !isParentDropdownExpanded },
                    ) {
                        OutlinedTextField(
                            value = selectedParentName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Parent category (optional)") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isParentDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                        )
                        ExposedDropdownMenu(
                            expanded = isParentDropdownExpanded,
                            onDismissRequest = { isParentDropdownExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (top level)") },
                                onClick = {
                                    selectedParentId = null
                                    isParentDropdownExpanded = false
                                },
                            )
                            existingCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        selectedParentId = category.id
                                        isParentDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedParentId) },
                enabled = name.isNotBlank(),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCategoryDialog(
    category: Category,
    allCategories: List<Category>,
    onConfirm: (name: String, parentId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(category.name) }
    var selectedParentId by remember { mutableStateOf(category.parentId) }
    var isParentDropdownExpanded by remember { mutableStateOf(false) }

    val forbidden = descendantIds(allCategories, category.id) + category.id
    val eligibleParents = allCategories.filter { it.id !in forbidden }
    val selectedParentName = eligibleParents.find { it.id == selectedParentId }?.name ?: "None"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ExposedDropdownMenuBox(
                    expanded = isParentDropdownExpanded,
                    onExpandedChange = { isParentDropdownExpanded = !isParentDropdownExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedParentName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Parent category (optional)") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isParentDropdownExpanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = isParentDropdownExpanded,
                        onDismissRequest = { isParentDropdownExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (top level)") },
                            onClick = {
                                selectedParentId = null
                                isParentDropdownExpanded = false
                            },
                        )
                        eligibleParents.forEach { parent ->
                            DropdownMenuItem(
                                text = { Text(parent.name) },
                                onClick = {
                                    selectedParentId = parent.id
                                    isParentDropdownExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedParentId) },
                enabled = name.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun descendantIds(categories: List<Category>, rootId: Long): Set<Long> {
    val result = mutableSetOf<Long>()
    val queue = ArrayDeque<Long>()
    queue.add(rootId)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        categories.filter { it.parentId == current }.forEach { child ->
            result.add(child.id)
            queue.add(child.id)
        }
    }
    return result
}

private fun buildCategoryTree(categories: List<Category>): List<Pair<Category, Int>> {
    val result = mutableListOf<Pair<Category, Int>>()

    fun visit(parentId: Long?, depth: Int) {
        categories.filter { it.parentId == parentId }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .forEach { cat ->
                result.add(cat to depth)
                visit(cat.id, depth + 1)
            }
    }
    visit(null, 0)
    return result
}
