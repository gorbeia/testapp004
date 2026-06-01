package com.example.testapp004.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.testapp004.BuildConfig
import com.example.testapp004.model.UpdateCheckOutcome
import com.example.testapp004.ui.screens.AcquaintanceDetailScreen
import com.example.testapp004.ui.screens.AcquaintancesListScreen
import com.example.testapp004.ui.screens.AddEditAcquaintanceScreen
import com.example.testapp004.ui.screens.CategoriesScreen
import com.example.testapp004.ui.screens.CategoryCanvasScreen
import com.example.testapp004.viewmodel.AcquaintanceDetailViewModel
import com.example.testapp004.viewmodel.AcquaintancesViewModel
import com.example.testapp004.viewmodel.AddEditAcquaintanceViewModel
import com.example.testapp004.viewmodel.CategoriesViewModel
import com.example.testapp004.viewmodel.CategoryCanvasViewModel
import com.example.testapp004.viewmodel.UpdateUiState
import com.example.testapp004.viewmodel.UpdateViewModel

sealed class Screen(val route: String) {
    object AcquaintancesList : Screen("acquaintances")

    object AcquaintanceDetail : Screen("acquaintance/{acquaintanceId}") {
        const val ARG_ACQUAINTANCE_ID = "acquaintanceId"

        fun createRoute(id: Long) = "acquaintance/$id"
    }

    object AddEditAcquaintance : Screen(
        "add_edit_acquaintance?acquaintanceId={acquaintanceId}" +
            "&preselectedCategoryId={preselectedCategoryId}" +
            "&returnToPersonId={returnToPersonId}",
    ) {
        const val ARG_ACQUAINTANCE_ID = "acquaintanceId"
        const val ARG_PRESELECTED_CATEGORY_ID = "preselectedCategoryId"
        const val ARG_RETURN_TO_PERSON_ID = "returnToPersonId"
        const val ROUTE_NEW = "add_edit_acquaintance"

        fun createRoute(id: Long) = "add_edit_acquaintance?acquaintanceId=$id"

        fun createRouteWithCategory(categoryId: Long) =
            "add_edit_acquaintance?preselectedCategoryId=$categoryId"

        fun createRouteForRelatedPerson(returnToPersonId: Long) =
            "add_edit_acquaintance?returnToPersonId=$returnToPersonId"
    }

    object Categories : Screen("categories")

    object CategoryCanvas : Screen("category_canvas/{categoryId}") {
        const val ARG_CATEGORY_ID = "categoryId"

        fun createRoute(id: Long) = "category_canvas/$id"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val acquaintancesViewModel: AcquaintancesViewModel = hiltViewModel()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val updateViewModel: UpdateViewModel = hiltViewModel()

    val updateState by updateViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        updateViewModel.installTrigger.collect { intent ->
            context.startActivity(intent)
        }
    }

    // contentWindowInsets = WindowInsets(0.dp) lets inner Scaffolds manage their own insets
    // (status bar via TopAppBar). innerPadding then only reflects the NavigationBar height.
    Scaffold(contentWindowInsets = WindowInsets(0.dp)) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            UpdateBanner(
                state = updateState,
                onUpdate = updateViewModel::downloadAndInstall,
                onDismiss = updateViewModel::dismissUpdate,
            )
            NavHost(
                navController = navController,
                startDestination = Screen.AcquaintancesList.route,
                modifier = Modifier.weight(1f),
            ) {
                composable(Screen.AcquaintancesList.route) {
                    AcquaintancesListScreen(
                        acquaintancesViewModel = acquaintancesViewModel,
                        categoriesViewModel = categoriesViewModel,
                        onPersonClick = { id ->
                            navController.navigate(Screen.AcquaintanceDetail.createRoute(id))
                        },
                        onAddPersonClick = { navController.navigate(Screen.AddEditAcquaintance.ROUTE_NEW) },
                        onManageCategoriesClick = { navController.navigate(Screen.Categories.route) },
                        onCheckForUpdatesClick = updateViewModel::openDebugDialog,
                        onCategoryClick = { navController.navigate(Screen.Categories.route) },
                        onCanvasClick = { id ->
                            navController.navigate(Screen.CategoryCanvas.createRoute(id))
                        },
                    )
                }
                composable(
                    route = Screen.AcquaintanceDetail.route,
                    arguments = listOf(
                        navArgument(Screen.AcquaintanceDetail.ARG_ACQUAINTANCE_ID) { type = NavType.LongType },
                    ),
                ) { backStackEntry ->
                    val detailViewModel: AcquaintanceDetailViewModel = hiltViewModel()

                    val pendingRelationWithId by backStackEntry.savedStateHandle
                        .getStateFlow<Long?>("pendingRelationWithId", null)
                        .collectAsState()

                    LaunchedEffect(pendingRelationWithId) {
                        val pendingId = pendingRelationWithId
                        if (pendingId != null) {
                            detailViewModel.openAddRelationDialogWithPreselectedPerson(pendingId)
                            backStackEntry.savedStateHandle.remove<Long>("pendingRelationWithId")
                        }
                    }

                    AcquaintanceDetailScreen(
                        viewModel = detailViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onEditClick = { id -> navController.navigate(Screen.AddEditAcquaintance.createRoute(id)) },
                        onPersonClick = { id -> navController.navigate(Screen.AcquaintanceDetail.createRoute(id)) },
                        onCategoryClick = { navController.navigate(Screen.Categories.route) },
                        onCreateNewPersonClick = {
                            navController.navigate(
                                Screen.AddEditAcquaintance.createRouteForRelatedPerson(
                                    detailViewModel.acquaintanceId,
                                ),
                            )
                        },
                    )
                }
                composable(
                    route = Screen.AddEditAcquaintance.route,
                    arguments = listOf(
                        navArgument(Screen.AddEditAcquaintance.ARG_ACQUAINTANCE_ID) {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                        navArgument(Screen.AddEditAcquaintance.ARG_PRESELECTED_CATEGORY_ID) {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                        navArgument(Screen.AddEditAcquaintance.ARG_RETURN_TO_PERSON_ID) {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                    ),
                ) { backStackEntry ->
                    val returnToPersonId = backStackEntry.arguments
                        ?.getLong(Screen.AddEditAcquaintance.ARG_RETURN_TO_PERSON_ID) ?: -1L
                    val addEditViewModel: AddEditAcquaintanceViewModel = hiltViewModel()
                    AddEditAcquaintanceScreen(
                        viewModel = addEditViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onPersonCreated = { newId ->
                            if (returnToPersonId != -1L) {
                                navController.previousBackStackEntry?.savedStateHandle
                                    ?.set("pendingRelationWithId", newId)
                                navController.popBackStack()
                            } else {
                                navController.navigate(Screen.AcquaintanceDetail.createRoute(newId)) {
                                    popUpTo(Screen.AddEditAcquaintance.route) { inclusive = true }
                                }
                            }
                        },
                    )
                }
                composable(Screen.Categories.route) {
                    CategoriesScreen(
                        viewModel = categoriesViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onCanvasClick = { id ->
                            navController.navigate(Screen.CategoryCanvas.createRoute(id))
                        },
                    )
                }
                composable(
                    route = Screen.CategoryCanvas.route,
                    arguments = listOf(
                        navArgument(Screen.CategoryCanvas.ARG_CATEGORY_ID) {
                            type = NavType.LongType
                        },
                    ),
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments
                        ?.getLong(Screen.CategoryCanvas.ARG_CATEGORY_ID) ?: -1L
                    val canvasViewModel: CategoryCanvasViewModel = hiltViewModel()
                    CategoryCanvasScreen(
                        viewModel = canvasViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onPersonClick = { id ->
                            navController.navigate(Screen.AcquaintanceDetail.createRoute(id))
                        },
                        onAddPersonClick = {
                            navController.navigate(
                                Screen.AddEditAcquaintance.createRouteWithCategory(categoryId),
                            )
                        },
                    )
                }
            }
        }
    }

    if (updateState.showDebugDialog) {
        UpdateDebugDialog(
            state = updateState,
            onCheckAgain = updateViewModel::checkForUpdate,
            onInstall = updateViewModel::downloadAndInstall,
            onDismiss = updateViewModel::closeDebugDialog,
        )
    }
}

@Composable
private fun UpdateDebugDialog(
    state: UpdateUiState,
    onCheckAgain: () -> Unit,
    onInstall: () -> Unit,
    onDismiss: () -> Unit,
) {
    val channel = if (BuildConfig.DEBUG) "debug (pre-release)" else "release"
    val apiUrl = if (BuildConfig.DEBUG) {
        "https://api.github.com/repos/gorbeia/testapp004/releases/tags/debug-latest"
    } else {
        "https://api.github.com/repos/gorbeia/testapp004/releases/latest"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update check") },
        text = {
            Column {
                DebugRow("Current version", BuildConfig.VERSION_NAME)
                DebugRow("Channel", channel)
                DebugRow("API URL", apiUrl)
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                when {
                    state.isChecking -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Checking…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    state.isDownloading -> {
                        Text("Downloading…", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { state.downloadProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = "${(state.downloadProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        val outcomeText = when (val o = state.lastCheckOutcome) {
                            is UpdateCheckOutcome.UpdateAvailable ->
                                "Update available: ${o.release.versionName}"
                            is UpdateCheckOutcome.UpToDate ->
                                "Up to date (remote: ${o.remoteVersion}, local: ${o.currentVersion})"
                            is UpdateCheckOutcome.NoAsset ->
                                "No APK asset found on release \"${o.tagName}\""
                            is UpdateCheckOutcome.HttpError ->
                                "HTTP ${o.code} — check GitHub release exists"
                            is UpdateCheckOutcome.NetworkError ->
                                "Network error: ${o.message}"
                            null -> "Not checked yet"
                        }
                        Text(
                            text = outcomeText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        )
                        state.error?.let { err ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (state.updateAvailable != null && !state.isDownloading && !state.isChecking) {
                Button(onClick = onInstall) { Text("Install") }
            } else if (!state.isChecking && !state.isDownloading) {
                TextButton(onClick = onCheckAgain) { Text("Check again") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun DebugRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
private fun UpdateBanner(
    state: UpdateUiState,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.updateAvailable == null && !state.isDownloading) return
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        if (state.isDownloading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Downloading update…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Update available",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "Version ${state.updateAvailable?.versionName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                TextButton(onClick = onUpdate) {
                    Text("Update")
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss update",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}
