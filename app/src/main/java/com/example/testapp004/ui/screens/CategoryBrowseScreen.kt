package com.example.testapp004.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.example.testapp004.model.Category
import com.example.testapp004.viewmodel.CategoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBrowseScreen(
    viewModel: CategoriesViewModel,
    onNavigateBack: () -> Unit,
    onCanvasClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedIds by remember { mutableStateOf(emptySet<Long>()) }
    val categories = uiState.categories
    val childrenOf = remember(categories) { categories.groupBy { it.parentId } }
    val roots = remember(categories) {
        categories.filter { it.parentId == null }.sortedBy { it.name }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Browse Categories") },
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
        if (roots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No categories yet",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val flatTree = remember(roots, childrenOf, expandedIds) {
                buildBrowseTree(roots, childrenOf, expandedIds)
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(flatTree, key = { it.category.id }) { item ->
                    val hasChildren = childrenOf[item.category.id]?.isNotEmpty() == true
                    CategoryBrowseRow(
                        category = item.category,
                        depth = item.depth,
                        hasChildren = hasChildren,
                        isExpanded = item.category.id in expandedIds,
                        onToggleExpand = {
                            expandedIds = if (item.category.id in expandedIds) {
                                expandedIds - item.category.id
                            } else {
                                expandedIds + item.category.id
                            }
                        },
                        onCanvasClick = { onCanvasClick(item.category.id) },
                    )
                }
            }
        }
    }
}

private data class BrowseTreeItem(val category: Category, val depth: Int)

private fun buildBrowseTree(
    roots: List<Category>,
    childrenOf: Map<Long?, List<Category>>,
    expandedIds: Set<Long>,
): List<BrowseTreeItem> {
    val result = mutableListOf<BrowseTreeItem>()
    fun visit(cats: List<Category>, depth: Int) {
        cats.forEach { cat ->
            result.add(BrowseTreeItem(cat, depth))
            if (cat.id in expandedIds) {
                val children = childrenOf[cat.id]?.sortedBy { it.name } ?: emptyList()
                visit(children, depth + 1)
            }
        }
    }
    visit(roots, 0)
    return result
}

@Composable
private fun CategoryBrowseRow(
    category: Category,
    depth: Int,
    hasChildren: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCanvasClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (hasChildren) Modifier.clickable(onClick = onToggleExpand) else Modifier)
            .padding(
                start = (16 + depth * 24).dp,
                end = 4.dp,
                top = 4.dp,
                bottom = 4.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasChildren) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(24.dp))
        }
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onCanvasClick) {
            Icon(
                imageVector = Icons.Default.Hub,
                contentDescription = "View canvas for ${category.name}",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = (16 + depth * 24).dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    )
}
