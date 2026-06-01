package com.example.testapp004.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.testapp004.model.Category
import com.example.testapp004.viewmodel.AddEditAcquaintanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAcquaintanceScreen(
    viewModel: AddEditAcquaintanceViewModel,
    onNavigateBack: () -> Unit,
    onPersonCreated: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            val savedId = uiState.savedId
            if (savedId != null) onPersonCreated(savedId) else onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "Edit Person" else "New Person") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = uiState.name.isNotBlank(),
                    ) {
                        Text(
                            text = "Save",
                            color = if (uiState.name.isNotBlank()) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = uiState.bio,
                onValueChange = viewModel::onBioChange,
                label = { Text("Bio (optional)") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            if (uiState.categories.isNotEmpty()) {
                CategoryMultiSelect(
                    categories = uiState.categories,
                    selectedIds = uiState.selectedCategoryIds,
                    onToggle = viewModel::onCategoryToggled,
                )
            }
        }
    }
}

@Composable
private fun CategoryMultiSelect(
    categories: List<Category>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    val treeOrder = buildCategoryTree(categories)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            treeOrder.forEachIndexed { index, (category, depth) ->
                if (index > 0) HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(category.id) }
                        .padding(
                            start = (4 + depth * 20).dp,
                            end = 8.dp,
                            top = 2.dp,
                            bottom = 2.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = category.id in selectedIds,
                        onCheckedChange = { onToggle(category.id) },
                    )
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

private fun buildCategoryTree(categories: List<Category>): List<Pair<Category, Int>> {
    val result = mutableListOf<Pair<Category, Int>>()

    fun visit(parentId: Long?, depth: Int) {
        categories.filter { it.parentId == parentId }.forEach { cat ->
            result.add(cat to depth)
            visit(cat.id, depth + 1)
        }
    }
    visit(null, 0)
    return result
}
