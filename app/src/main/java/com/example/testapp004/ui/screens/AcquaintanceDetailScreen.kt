package com.example.testapp004.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.testapp004.model.Acquaintance
import com.example.testapp004.model.Category
import com.example.testapp004.model.ContactInfo
import com.example.testapp004.model.RelationCategory
import com.example.testapp004.model.RelationTypeOption
import com.example.testapp004.model.RelationTypes
import com.example.testapp004.viewmodel.AcquaintanceDetailViewModel
import com.example.testapp004.viewmodel.RelationDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcquaintanceDetailScreen(
    viewModel: AcquaintanceDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditClick: (Long) -> Unit,
    onPersonClick: (Long) -> Unit,
    onCategoryClick: () -> Unit,
    onCreateNewPersonClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val acquaintance = uiState.acquaintance
    var showDeleteConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(acquaintance) {
        if (acquaintance == null && !uiState.isLoading) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(acquaintance?.name ?: "") },
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
                    IconButton(onClick = { acquaintance?.let { onEditClick(it.id) } }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        if (acquaintance != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    BioSection(
                        bio = acquaintance.bio,
                        categoryNames = uiState.categoryNames,
                        onCategoryClick = onCategoryClick,
                        onSaveBio = viewModel::saveBio,
                    )
                }
                item {
                    LinkedContactSection(
                        linkedContactInfo = uiState.linkedContactInfo,
                        isLinked = acquaintance.androidContactLookupKey != null,
                        onLinkContact = { viewModel.linkContact(it) },
                        onUnlinkContact = { viewModel.unlinkContact() },
                    )
                }
                item {
                    RelationsSection(
                        relations = uiState.relations,
                        onAddClick = viewModel::openAddRelationDialog,
                        onDelete = viewModel::deleteRelation,
                        onPersonClick = onPersonClick,
                    )
                }
            }
        }
    }

    if (uiState.isAddRelationDialogOpen) {
        AddRelationDialog(
            currentPersonName = uiState.acquaintance?.name ?: "",
            currentPersonCategoryIds = uiState.acquaintance?.categoryIds ?: emptySet(),
            allAcquaintances = uiState.allOtherAcquaintances,
            allCategories = uiState.allCategories,
            preselectedPersonId = uiState.pendingNewRelationPersonId,
            onConfirm = { otherId, typeKey, isCurrentPersonFrom, customLabel ->
                viewModel.addRelation(otherId, typeKey, isCurrentPersonFrom, customLabel)
            },
            onDismiss = viewModel::closeAddRelationDialog,
            onCreateNewPerson = {
                viewModel.closeAddRelationDialog()
                onCreateNewPersonClick()
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${acquaintance?.name ?: "person"}?") },
            text = { Text("This will permanently delete this person and all their relations.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteAcquaintance()
                        onNavigateBack()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun BioSection(
    bio: String,
    categoryNames: List<String>,
    onCategoryClick: () -> Unit,
    onSaveBio: (String) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var draft by remember(bio) { mutableStateOf(bio) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (categoryNames.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    categoryNames.forEach { name ->
                        SuggestionChip(onClick = onCategoryClick, label = { Text(name) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (isEditing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Bio") },
                    minLines = 3,
                    maxLines = 8,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    TextButton(onClick = {
                        isEditing = false
                        draft = bio
                    }) { Text("Cancel") }
                    Button(onClick = {
                        onSaveBio(draft)
                        isEditing = false
                    }) { Text("Save") }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (bio.isNotBlank()) {
                        Text(
                            text = bio,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Text(
                            text = "No bio added",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit bio")
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedContactSection(
    linkedContactInfo: ContactInfo?,
    isLinked: Boolean,
    onLinkContact: (String) -> Unit,
    onUnlinkContact: () -> Unit,
) {
    val context = LocalContext.current

    val contactPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickContact(),
    ) { uri ->
        uri?.let { onLinkContact(it.toString()) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) contactPickerLauncher.launch(null)
    }

    fun launchPicker() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            contactPickerLauncher.launch(null)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        if (!isLinked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Linked Contact",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "None",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { launchPicker() }) { Text("Link Contact") }
            }
        } else {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Linked Contact",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (linkedContactInfo != null) {
                    Text(
                        text = linkedContactInfo.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (linkedContactInfo.primaryPhone != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = linkedContactInfo.primaryPhone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val lookupUri = Uri.withAppendedPath(
                                    ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                                    linkedContactInfo.lookupKey,
                                )
                                context.startActivity(Intent(Intent.ACTION_VIEW, lookupUri))
                            },
                        ) {
                            Text("Open Contact")
                        }
                        if (linkedContactInfo.primaryPhone != null) {
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_DIAL,
                                            Uri.parse("tel:${linkedContactInfo.primaryPhone}"),
                                        ),
                                    )
                                },
                            ) {
                                Text("Call")
                            }
                        }
                    }
                    if (linkedContactInfo.primaryPhone != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                val phone = linkedContactInfo.primaryPhone.filter { it.isDigit() }
                                try {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("whatsapp://send?phone=$phone"),
                                        ),
                                    )
                                } catch (_: ActivityNotFoundException) {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://wa.me/$phone"),
                                        ),
                                    )
                                }
                            },
                        ) {
                            Text("WhatsApp")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { launchPicker() }) {
                            Text("Change")
                        }
                        TextButton(onClick = onUnlinkContact) {
                            Text(
                                text = "Unlink",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                } else {
                    // Linked but contact info still loading or contact was deleted from device
                    Text(
                        text = "Contact unavailable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onUnlinkContact) {
                        Text(
                            text = "Unlink",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RelationsSection(
    relations: List<RelationDisplay>,
    onAddClick: () -> Unit,
    onDelete: (Long) -> Unit,
    onPersonClick: (Long) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Relations", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add relation")
            }
        }
        HorizontalDivider()
        if (relations.isEmpty()) {
            Text(
                text = "No relations yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                relations.forEachIndexed { index, relation ->
                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    RelationRow(
                        relation = relation,
                        onDelete = { onDelete(relation.relationId) },
                        onPersonClick = { onPersonClick(relation.otherPersonId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RelationRow(
    relation: RelationDisplay,
    onDelete: () -> Unit,
    onPersonClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TextButton(
                onClick = onPersonClick,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = relation.otherPersonName,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            Text(
                text = relation.counterpartLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove relation",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRelationDialog(
    currentPersonName: String,
    currentPersonCategoryIds: Set<Long>,
    allAcquaintances: List<Acquaintance>,
    allCategories: List<Category>,
    preselectedPersonId: Long?,
    onConfirm: (otherId: Long, typeKey: String, isCurrentPersonFrom: Boolean, customLabel: String?) -> Unit,
    onDismiss: () -> Unit,
    onCreateNewPerson: () -> Unit,
) {
    val typeOptions = remember { RelationTypes.typeOptions() }

    var selectedPersonId by remember(preselectedPersonId) { mutableStateOf<Long?>(preselectedPersonId) }
    var selectedOption by remember { mutableStateOf<RelationTypeOption?>(null) }
    var customLabel by remember { mutableStateOf("") }
    var filterText by remember { mutableStateOf("") }
    var isPersonDropdownExpanded by remember { mutableStateOf(false) }
    var isTypeDropdownExpanded by remember { mutableStateOf(false) }

    val selectedPersonName = allAcquaintances.find { it.id == selectedPersonId }?.name ?: ""
    val isCustom = selectedOption?.typeKey == RelationTypes.CUSTOM_KEY
    val isConfirmEnabled = selectedPersonId != null &&
        selectedOption != null &&
        (!isCustom || customLabel.isNotBlank())
    val counterpartLabel: String? = selectedOption?.takeIf { !isCustom }?.let { opt ->
        RelationTypes.findByKey(opt.typeKey)?.let { type ->
            if (opt.isCurrentPersonFrom) type.toLabel else type.fromLabel
        }
    }
    val roleLabel = if (currentPersonName.isNotEmpty()) "$currentPersonName's role" else "Relation type"

    val filteredSorted = remember(allAcquaintances, filterText, currentPersonCategoryIds) {
        val base = if (filterText.isBlank()) {
            allAcquaintances
        } else {
            allAcquaintances.filter { it.name.contains(filterText, ignoreCase = true) }
        }
        base.sortedWith(
            compareByDescending<Acquaintance> { p ->
                currentPersonCategoryIds.isNotEmpty() && p.categoryIds.any { it in currentPersonCategoryIds }
            }.thenBy { it.name },
        )
    }

    val sharedPersons = remember(filteredSorted, currentPersonCategoryIds) {
        filteredSorted.filter { p ->
            currentPersonCategoryIds.isNotEmpty() && p.categoryIds.any { it in currentPersonCategoryIds }
        }
    }
    val otherPersons = remember(filteredSorted, currentPersonCategoryIds) {
        filteredSorted.filter { p ->
            currentPersonCategoryIds.isEmpty() || p.categoryIds.none { it in currentPersonCategoryIds }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Relation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = isPersonDropdownExpanded,
                    onExpandedChange = { expanded ->
                        isPersonDropdownExpanded = expanded
                        if (!expanded) filterText = ""
                    },
                ) {
                    OutlinedTextField(
                        value = if (isPersonDropdownExpanded) filterText else selectedPersonName,
                        onValueChange = { filterText = it; isPersonDropdownExpanded = true },
                        label = { Text("Person") },
                        placeholder = { Text("Search…") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPersonDropdownExpanded)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = isPersonDropdownExpanded,
                        onDismissRequest = { isPersonDropdownExpanded = false; filterText = "" },
                    ) {
                        if (sharedPersons.isNotEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Same groups",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                onClick = {},
                                enabled = false,
                            )
                            sharedPersons.forEach { person ->
                                val categoryNames = person.categoryIds
                                    .mapNotNull { cId -> allCategories.find { it.id == cId }?.name }
                                    .sorted()
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(person.name)
                                            if (categoryNames.isNotEmpty()) {
                                                Text(
                                                    text = categoryNames.joinToString(", "),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPersonId = person.id
                                        isPersonDropdownExpanded = false
                                        filterText = ""
                                    },
                                )
                            }
                        }
                        if (otherPersons.isNotEmpty()) {
                            if (sharedPersons.isNotEmpty()) {
                                HorizontalDivider()
                            }
                            otherPersons.forEach { person ->
                                val categoryNames = person.categoryIds
                                    .mapNotNull { cId -> allCategories.find { it.id == cId }?.name }
                                    .sorted()
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(person.name)
                                            if (categoryNames.isNotEmpty()) {
                                                Text(
                                                    text = categoryNames.joinToString(", "),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedPersonId = person.id
                                        isPersonDropdownExpanded = false
                                        filterText = ""
                                    },
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = onCreateNewPerson,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                    )
                    Text("Create new person")
                }

                ExposedDropdownMenuBox(
                    expanded = isTypeDropdownExpanded,
                    onExpandedChange = { isTypeDropdownExpanded = !isTypeDropdownExpanded },
                ) {
                    OutlinedTextField(
                        value = selectedOption?.displayLabel ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(roleLabel) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTypeDropdownExpanded) },
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
                if (counterpartLabel != null && selectedPersonName.isNotEmpty()) {
                    Text(
                        text = "→ $selectedPersonName will appear as: $counterpartLabel",
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
                    val pid = selectedPersonId ?: return@TextButton
                    val opt = selectedOption ?: return@TextButton
                    onConfirm(pid, opt.typeKey, opt.isCurrentPersonFrom, customLabel.takeIf { isCustom })
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
