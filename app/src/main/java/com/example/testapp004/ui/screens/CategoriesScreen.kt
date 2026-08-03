package com.example.testapp004.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.testapp004.model.Category
import com.example.testapp004.viewmodel.CategoriesViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
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
            val density = LocalDensity.current
            val indentStepPx = with(density) { 24.dp.toPx() }

            val parentIds = remember(uiState.categories) {
                uiState.categories.mapNotNull { it.parentId }.toSet()
            }

            // Working copy of the tree for live drag animation; resets when ViewModel emits.
            var localTree by remember(uiState.categories, uiState.collapsedCategories) {
                mutableStateOf(buildCategoryTree(uiState.categories, uiState.collapsedCategories))
            }

            // Drag state
            var draggingId by remember { mutableStateOf<Long?>(null) }
            var dragXOffset by remember { mutableStateOf(0f) }
            var dragStartDepth by remember { mutableStateOf(0) }

            // Derived: position of dragging item in the live tree.
            val draggingIdx = if (draggingId != null) {
                localTree.indexOfFirst { it.first.id == draggingId }
            } else {
                -1
            }
            val itemAboveDepth = if (draggingIdx > 0) localTree[draggingIdx - 1].second else -1
            val maxAllowedDepth = (itemAboveDepth + 1).coerceAtLeast(0)
            val proposedDepth = if (draggingId != null) {
                (dragStartDepth + (dragXOffset / indentStepPx).roundToInt())
                    .coerceIn(0, maxAllowedDepth)
            } else {
                0
            }
            val proposedParentId: Long? = if (draggingId == null || proposedDepth == 0) {
                null
            } else {
                findParentIdAtDepth(localTree, draggingIdx, proposedDepth)
            }

            val listState = rememberLazyListState()
            val reorderState = rememberReorderableLazyListState(listState) { from, to ->
                localTree = localTree.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(localTree, key = { _, (cat, _) -> cat.id }) { _, (category, depth) ->
                    ReorderableItem(reorderState, key = category.id) { isDragging ->
                        val isThisDragging = isDragging
                        val isProposedParent = proposedParentId == category.id && !isThisDragging
                        val displayDepth = if (isThisDragging) proposedDepth else depth

                        // Build the handle modifier here so draggableHandle is in scope.
                        val handleModifier = Modifier
                            .pointerInput(category.id) {
                                // Initial pass: read X before the library's Main-pass consumes events.
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull() ?: continue
                                        if (change.pressed && draggingId == category.id) {
                                            dragXOffset += (change.position - change.previousPosition).x
                                        }
                                    }
                                }
                            }
                            .draggableHandle(
                                onDragStarted = {
                                    draggingId = category.id
                                    dragStartDepth = depth
                                    dragXOffset = 0f
                                },
                                onDragStopped = {
                                    val fromId = draggingId ?: return@draggableHandle
                                    val tree = localTree
                                    val fromIdx = tree.indexOfFirst { it.first.id == fromId }
                                    if (fromIdx != -1) {
                                        val aboveDepth =
                                            if (fromIdx > 0) tree[fromIdx - 1].second else -1
                                        val maxDepth = (aboveDepth + 1).coerceAtLeast(0)
                                        val depthDelta = (dragXOffset / indentStepPx).roundToInt()
                                        val finalDepth =
                                            (dragStartDepth + depthDelta).coerceIn(0, maxDepth)
                                        val newParentId =
                                            findParentIdAtDepth(tree, fromIdx, finalDepth)
                                        // First existing child of newParentId after the drop
                                        // position becomes the insert-before anchor.
                                        val targetPositionId = tree.drop(fromIdx + 1)
                                            .map { it.first }
                                            .firstOrNull { it.parentId == newParentId }
                                            ?.id
                                        viewModel.moveCategory(fromId, newParentId, targetPositionId)
                                    }
                                    draggingId = null
                                    dragXOffset = 0f
                                },
                            )

                        CategoryItem(
                            category = category,
                            depth = displayDepth,
                            isDragging = isThisDragging,
                            isProposedParent = isProposedParent,
                            hasChildren = category.id in parentIds,
                            isCollapsed = category.id in uiState.collapsedCategories,
                            handleModifier = handleModifier,
                            onToggleCollapse = { viewModel.toggleCollapsed(category.id) },
                            onAddChild = { viewModel.openAddChildDialog(category) },
                            onEdit = { viewModel.openEditDialog(category) },
                            onDelete = { viewModel.deleteCategory(category.id) },
                        )
                    }
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
    isProposedParent: Boolean,
    hasChildren: Boolean,
    isCollapsed: Boolean,
    handleModifier: Modifier,
    onToggleCollapse: () -> Unit,
    onAddChild: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 24).dp)
            .zIndex(if (isDragging) 1f else 0f)
            .alpha(if (isDragging) 0.85f else 1f),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 2.dp,
        ),
        colors = when {
            isProposedParent -> CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
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
                modifier = handleModifier.padding(horizontal = 4.dp),
            )
            if (depth > 0) {
                Text(
                    text = "└ ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (hasChildren) {
                IconButton(onClick = onToggleCollapse) {
                    Icon(
                        imageVector = if (isCollapsed) {
                            Icons.Default.KeyboardArrowRight
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (isCollapsed) "Expand" else "Collapse",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
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
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = isParentDropdownExpanded,
                                )
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
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = isParentDropdownExpanded,
                            )
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

/**
 * Walk backward from [insertionIdx]-1 to find the nearest ancestor at depth [depth]-1.
 * Returns null if [depth] == 0 (root) or no ancestor found (capped by a shallower item).
 */
private fun findParentIdAtDepth(
    tree: List<Pair<Category, Int>>,
    insertionIdx: Int,
    depth: Int,
): Long? {
    if (depth == 0) return null
    val targetDepth = depth - 1
    for (i in (insertionIdx - 1) downTo 0) {
        val (cat, d) = tree[i]
        if (d == targetDepth) return cat.id
        if (d < targetDepth) return null
    }
    return null
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

private fun buildCategoryTree(
    categories: List<Category>,
    collapsed: Set<Long> = emptySet(),
): List<Pair<Category, Int>> {
    val result = mutableListOf<Pair<Category, Int>>()

    fun visit(parentId: Long?, depth: Int) {
        categories.filter { it.parentId == parentId }
            .sortedWith(compareBy({ it.sortOrder }, { it.id }))
            .forEach { cat ->
                result.add(cat to depth)
                if (cat.id !in collapsed) {
                    visit(cat.id, depth + 1)
                }
            }
    }
    visit(null, 0)
    return result
}
