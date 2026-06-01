package com.example.testapp004.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.testapp004.model.Acquaintance
import com.example.testapp004.model.Category
import com.example.testapp004.viewmodel.AcquaintancesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcquaintancesListScreen(
    viewModel: AcquaintancesViewModel,
    onPersonClick: (Long) -> Unit,
    onAddPersonClick: () -> Unit,
    onManageCategoriesClick: () -> Unit,
    onCheckForUpdatesClick: () -> Unit,
    onCategoryClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("People") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    IconButton(onClick = onManageCategoriesClick) {
                        Icon(Icons.Default.List, contentDescription = "Manage categories")
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPersonClick) {
                Icon(Icons.Default.Add, contentDescription = "Add person")
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name or bio") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
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
                    onCategorySelected = viewModel::selectCategory,
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
                    onDelete = viewModel::deleteAcquaintance,
                    onCategoryClick = onCategoryClick,
                    modifier = Modifier.weight(1f),
                )
            }
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

    // Walk up to find the root ancestor (handles up to arbitrary depth, stops at root)
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
    onDelete: (Long) -> Unit,
    onCategoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(acquaintances, key = { it.id }) { person ->
            AcquaintanceCard(
                acquaintance = person,
                categoryNames = categories.filter { it.id in person.categoryIds }.map { it.name },
                onClick = { onPersonClick(person.id) },
                onDelete = { onDelete(person.id) },
                onCategoryClick = onCategoryClick,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcquaintanceCard(
    acquaintance: Acquaintance,
    categoryNames: List<String>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCategoryClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = acquaintance.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (acquaintance.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = acquaintance.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (categoryNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
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
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete person",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
