package com.example.testapp004.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.testapp004.model.Category
import com.example.testapp004.viewmodel.ImportContactItem
import com.example.testapp004.viewmodel.ImportContactsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportContactsScreen(
    viewModel: ImportContactsViewModel,
    onNavigateBack: () -> Unit,
    onImportDone: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        permissionGranted = granted
        if (granted) viewModel.loadContacts()
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) viewModel.loadContacts()
    }

    LaunchedEffect(uiState.importedCount) {
        if (uiState.importedCount != null) onImportDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import from contacts") },
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
                    if (uiState.contacts.isNotEmpty()) {
                        val allSelectable = uiState.contacts.filter { !it.isAlreadyLinked }
                        if (uiState.selectedLookupKeys.size == allSelectable.size && allSelectable.isNotEmpty()) {
                            TextButton(onClick = viewModel::onDeselectAll) { Text("Deselect all") }
                        } else {
                            TextButton(onClick = viewModel::onSelectAll) { Text("Select all") }
                        }
                    }
                },
            )
        },
    ) { paddingValues ->
        when {
            !permissionGranted -> {
                PermissionRequiredContent(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                )
            }
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = viewModel::loadContacts) { Text("Retry") }
                    }
                }
            }
            else -> {
                ContactsListContent(
                    contacts = uiState.contacts,
                    selectedLookupKeys = uiState.selectedLookupKeys,
                    categories = uiState.categories,
                    selectedCategoryIds = uiState.selectedCategoryIds,
                    onContactToggled = viewModel::onContactToggled,
                    onCategoryToggled = viewModel::onCategoryToggled,
                    onImport = viewModel::importSelected,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                )
            }
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ContactPage,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Contacts permission required",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Allow this app to read your contacts to import them as people.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) { Text("Grant permission") }
    }
}

@Composable
private fun ContactsListContent(
    contacts: List<ImportContactItem>,
    selectedLookupKeys: Set<String>,
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onContactToggled: (String) -> Unit,
    onCategoryToggled: (Long) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedCount = selectedLookupKeys.size
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        if (contacts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No contacts found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            if (categories.isNotEmpty()) {
                item {
                    CategoryPickerSection(
                        categories = categories,
                        selectedCategoryIds = selectedCategoryIds,
                        onCategoryToggled = onCategoryToggled,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item { HorizontalDivider() }
            }
            items(contacts, key = { it.lookupKey }) { contact ->
                ContactImportRow(
                    contact = contact,
                    isSelected = contact.lookupKey in selectedLookupKeys,
                    onToggle = { onContactToggled(contact.lookupKey) },
                )
                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Button(
                        onClick = onImport,
                        enabled = selectedCount > 0,
                    ) {
                        Text(if (selectedCount > 0) "Import $selectedCount" else "Import")
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryPickerSection(
    categories: List<Category>,
    selectedCategoryIds: Set<Long>,
    onCategoryToggled: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Assign to categories (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            val roots = remember(categories) { categories.filter { it.parentId == null }.sortedBy { it.sortOrder } }
            val childrenOf = remember(categories) { categories.groupBy { it.parentId } }
            CategoryTreePicker(
                nodes = roots,
                childrenOf = childrenOf,
                selectedIds = selectedCategoryIds,
                onToggle = onCategoryToggled,
                depth = 0,
            )
        }
    }
}

@Composable
private fun CategoryTreePicker(
    nodes: List<Category>,
    childrenOf: Map<Long?, List<Category>>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    depth: Int,
) {
    nodes.forEach { category ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (depth * 16).dp),
        ) {
            Checkbox(
                checked = category.id in selectedIds,
                onCheckedChange = { onToggle(category.id) },
            )
            Text(category.name, style = MaterialTheme.typography.bodyMedium)
        }
        val children = childrenOf[category.id]?.sortedBy { it.sortOrder } ?: emptyList()
        if (children.isNotEmpty()) {
            CategoryTreePicker(
                nodes = children,
                childrenOf = childrenOf,
                selectedIds = selectedIds,
                onToggle = onToggle,
                depth = depth + 1,
            )
        }
    }
}

@Composable
private fun ContactImportRow(
    contact: ImportContactItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    val dimmed = contact.isAlreadyLinked
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dimmed) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Already linked",
                modifier = Modifier
                    .size(24.dp)
                    .padding(2.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        } else {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (dimmed) {
                Text(
                    text = "Already linked",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                val details = buildList {
                    contact.birthday?.let { add(formatBirthday(it)) }
                    if (contact.bio.isNotBlank()) add(contact.bio.lines().first().take(60))
                }
                if (details.isNotEmpty()) {
                    Text(
                        text = details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (contact.birthday != null && !dimmed) {
            Icon(
                imageVector = Icons.Default.Cake,
                contentDescription = "Has birthday",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatBirthday(birthday: String): String {
    return try {
        if (birthday.startsWith("--")) {
            val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
            val out = SimpleDateFormat("MMMM d", Locale.getDefault())
            out.format(sdf.parse(birthday.substring(2)) ?: return birthday)
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val out = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            out.format(sdf.parse(birthday) ?: return birthday)
        }
    } catch (e: Exception) {
        birthday
    }
}
