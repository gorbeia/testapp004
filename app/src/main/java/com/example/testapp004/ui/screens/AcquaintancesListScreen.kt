package com.example.testapp004.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.testapp004.model.Acquaintance
import com.example.testapp004.model.Category
import com.example.testapp004.viewmodel.AcquaintancesUiState
import com.example.testapp004.viewmodel.AcquaintancesViewModel
import com.example.testapp004.viewmodel.CategoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcquaintancesListScreen(
    acquaintancesViewModel: AcquaintancesViewModel,
    categoriesViewModel: CategoriesViewModel,
    onPersonClick: (Long) -> Unit,
    onAddPersonClick: () -> Unit,
    onImportFromContactsClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onCheckForUpdatesClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onCanvasClick: (Long) -> Unit,
) {
    val acquaintancesState by acquaintancesViewModel.uiState.collectAsState()
    val categoriesState by categoriesViewModel.uiState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val allCategories = categoriesState.categories
    val childrenOf = remember(allCategories) { allCategories.groupBy { it.parentId } }
    val roots = remember(allCategories) {
        allCategories.filter { it.parentId == null }.sortedBy { it.name }
    }
    val expandedIds = acquaintancesState.expandedCategoryIds
    val flatTree = remember(roots, childrenOf, expandedIds) {
        buildBrowseTree(roots, childrenOf, expandedIds)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Acquaintances") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    actions = {
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Import from contacts") },
                                    onClick = {
                                        menuExpanded = false
                                        onImportFromContactsClick()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Manage categories") },
                                    onClick = {
                                        menuExpanded = false
                                        onManageCategoriesClick()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Check for updates") },
                                    onClick = {
                                        menuExpanded = false
                                        onCheckForUpdatesClick()
                                    },
                                )
                            }
                        }
                    },
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Categories") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AccountTree,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("People") },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(onClick = onAddPersonClick) {
                    Icon(Icons.Default.Add, contentDescription = "Add person")
                }
            }
        },
    ) { paddingValues ->
        when (selectedTab) {
            0 -> CategoryBrowseContent(
                roots = roots,
                flatTree = flatTree,
                childrenOf = childrenOf,
                expandedIds = expandedIds,
                onToggleExpand = acquaintancesViewModel::toggleCategoryExpanded,
                onCanvasClick = onCanvasClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
            else -> PeopleContent(
                uiState = acquaintancesState,
                onSearchQueryChange = acquaintancesViewModel::updateSearchQuery,
                onCategorySelected = acquaintancesViewModel::selectCategory,
                onPersonClick = onPersonClick,
                onCategoryClick = onCategoryClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        }
    }
}

@Composable
private fun CategoryBrowseContent(
    roots: List<Category>,
    flatTree: List<BrowseTreeItem>,
    childrenOf: Map<Long?, List<Category>>,
    expandedIds: Set<Long>,
    onToggleExpand: (Long) -> Unit,
    onCanvasClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (roots.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No categories yet",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(flatTree, key = { it.category.id }) { item ->
                val hasChildren = childrenOf[item.category.id]?.isNotEmpty() == true
                CategoryBrowseRow(
                    category = item.category,
                    depth = item.depth,
                    hasChildren = hasChildren,
                    isExpanded = item.category.id in expandedIds,
                    onToggleExpand = { onToggleExpand(item.category.id) },
                    onCanvasClick = { onCanvasClick(item.category.id) },
                )
            }
        }
    }
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
private fun PeopleContent(
    uiState: AcquaintancesUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onPersonClick: (Long) -> Unit,
    onCategoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search by name or bio") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
        )
        if (uiState.categories.isNotEmpty()) {
            CategoryFilterRow(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelected = onCategorySelected,
            )
        }
        if (uiState.acquaintances.isEmpty()) {
            AcquaintancesEmptyState(
                hasFilter = uiState.selectedCategoryId != null,
                searchQuery = uiState.searchQuery,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            )
        } else {
            AcquaintancesList(
                acquaintances = uiState.acquaintances,
                categories = uiState.categories,
                onPersonClick = onPersonClick,
                onCategoryClick = onCategoryClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit,
) {
    val rootCategories = categories.filter { it.parentId == null }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    fun rootOf(category: Category): Category {
        val parent = categories.find { it.id == category.parentId } ?: return category
        return rootOf(parent)
    }

    val activeRoot: Category? = selectedCategory?.let { rootOf(it) }
    val childCategories = if (activeRoot != null) {
        categories.filter { it.parentId == activeRoot.id }
    } else {
        emptyList()
    }

    Column {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("All") },
                )
            }
            items(rootCategories, key = { it.id }) { category ->
                FilterChip(
                    selected = category.id == activeRoot?.id,
                    onClick = { onCategorySelected(category.id) },
                    label = { Text(category.name) },
                )
            }
        }
        if (childCategories.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(start = 32.dp, end = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == activeRoot?.id,
                        onClick = { onCategorySelected(activeRoot?.id) },
                        label = { Text("All ${activeRoot?.name}") },
                    )
                }
                items(childCategories, key = { it.id }) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AcquaintancesEmptyState(
    hasFilter: Boolean,
    searchQuery: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            val message = when {
                searchQuery.isNotBlank() && hasFilter ->
                    "No results for \"$searchQuery\" in this category"
                searchQuery.isNotBlank() -> "No results for \"$searchQuery\""
                hasFilter -> "No people in this category"
                else -> "No people yet"
            }
            Text(
                text = message,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (!hasFilter && searchQuery.isBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap + to add your first person",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AcquaintancesList(
    acquaintances: List<Acquaintance>,
    categories: List<Category>,
    onPersonClick: (Long) -> Unit,
    onCategoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 80.dp),
    ) {
        items(acquaintances, key = { it.id }) { person ->
            AcquaintanceCard(
                acquaintance = person,
                categoryNames = categories.filter { it.id in person.categoryIds }.map { it.name },
                onClick = { onPersonClick(person.id) },
                onCategoryClick = onCategoryClick,
            )
        }
    }
}

@Composable
private fun AcquaintanceCard(
    acquaintance: Acquaintance,
    categoryNames: List<String>,
    onClick: () -> Unit,
    onCategoryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (acquaintance.isDeceased) "${acquaintance.name} †" else acquaintance.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (acquaintance.isDeceased) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (acquaintance.bio.isNotBlank()) {
                    Text(
                        text = acquaintance.bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (categoryNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        categoryNames.forEach { name ->
                            SuggestionChip(
                                onClick = onCategoryClick,
                                label = { Text(name, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                }
            }
            if (acquaintance.androidContactLookupKey != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Linked to Android contact",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}
