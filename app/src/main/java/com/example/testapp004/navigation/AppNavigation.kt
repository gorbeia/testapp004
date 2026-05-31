package com.example.testapp004.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.testapp004.ui.screens.AcquaintanceDetailScreen
import com.example.testapp004.ui.screens.AcquaintancesListScreen
import com.example.testapp004.ui.screens.AddEditAcquaintanceScreen
import com.example.testapp004.ui.screens.CategoriesScreen
import com.example.testapp004.ui.screens.EditNoteScreen
import com.example.testapp004.ui.screens.HomeScreen
import com.example.testapp004.viewmodel.AcquaintanceDetailViewModel
import com.example.testapp004.viewmodel.AcquaintancesViewModel
import com.example.testapp004.viewmodel.AddEditAcquaintanceViewModel
import com.example.testapp004.viewmodel.CategoriesViewModel
import com.example.testapp004.viewmodel.NotesViewModel
import com.example.testapp004.viewmodel.UpdateUiState
import com.example.testapp004.viewmodel.UpdateViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")

    object EditNote : Screen("edit_note/{noteId}") {
        const val ARG_NOTE_ID = "noteId"

        fun createRoute(noteId: Long) = "edit_note/$noteId"
    }

    object AcquaintancesList : Screen("acquaintances")

    object AcquaintanceDetail : Screen("acquaintance/{acquaintanceId}") {
        const val ARG_ACQUAINTANCE_ID = "acquaintanceId"

        fun createRoute(id: Long) = "acquaintance/$id"
    }

    object AddEditAcquaintance : Screen("add_edit_acquaintance?acquaintanceId={acquaintanceId}") {
        const val ARG_ACQUAINTANCE_ID = "acquaintanceId"
        const val ROUTE_NEW = "add_edit_acquaintance"

        fun createRoute(id: Long) = "add_edit_acquaintance?acquaintanceId=$id"
    }

    object Categories : Screen("categories")
}

private val bottomNavTabs = setOf(Screen.Home.route, Screen.AcquaintancesList.route)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val notesViewModel: NotesViewModel = hiltViewModel()
    val acquaintancesViewModel: AcquaintancesViewModel = hiltViewModel()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()
    val updateViewModel: UpdateViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val updateState by updateViewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        updateViewModel.installTrigger.collect { intent ->
            context.startActivity(intent)
        }
    }

    // contentWindowInsets = WindowInsets(0.dp) lets inner Scaffolds manage their own insets
    // (status bar via TopAppBar). innerPadding then only reflects the NavigationBar height.
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            if (currentRoute in bottomNavTabs) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Notes") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.AcquaintancesList.route,
                        onClick = {
                            navController.navigate(Screen.AcquaintancesList.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("People") },
                    )
                }
            }
        },
    ) { innerPadding ->
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
                startDestination = Screen.Home.route,
                modifier = Modifier.weight(1f),
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = notesViewModel,
                        onNoteClick = { noteId -> navController.navigate(Screen.EditNote.createRoute(noteId)) },
                    )
                }
                composable(
                    route = Screen.EditNote.route,
                    arguments = listOf(navArgument(Screen.EditNote.ARG_NOTE_ID) { type = NavType.LongType }),
                ) { backStackEntry ->
                    val noteId = backStackEntry.arguments?.getLong(Screen.EditNote.ARG_NOTE_ID) ?: return@composable
                    EditNoteScreen(
                        noteId = noteId,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(Screen.AcquaintancesList.route) {
                    AcquaintancesListScreen(
                        viewModel = acquaintancesViewModel,
                        onPersonClick = { id -> navController.navigate(Screen.AcquaintanceDetail.createRoute(id)) },
                        onAddPersonClick = { navController.navigate(Screen.AddEditAcquaintance.ROUTE_NEW) },
                        onManageCategoriesClick = { navController.navigate(Screen.Categories.route) },
                    )
                }
                composable(
                    route = Screen.AcquaintanceDetail.route,
                    arguments = listOf(
                        navArgument(Screen.AcquaintanceDetail.ARG_ACQUAINTANCE_ID) { type = NavType.LongType },
                    ),
                ) {
                    val detailViewModel: AcquaintanceDetailViewModel = hiltViewModel()
                    AcquaintanceDetailScreen(
                        viewModel = detailViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onEditClick = { id -> navController.navigate(Screen.AddEditAcquaintance.createRoute(id)) },
                        onPersonClick = { id -> navController.navigate(Screen.AcquaintanceDetail.createRoute(id)) },
                    )
                }
                composable(
                    route = Screen.AddEditAcquaintance.route,
                    arguments = listOf(
                        navArgument(Screen.AddEditAcquaintance.ARG_ACQUAINTANCE_ID) {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                    ),
                ) {
                    val addEditViewModel: AddEditAcquaintanceViewModel = hiltViewModel()
                    AddEditAcquaintanceScreen(
                        viewModel = addEditViewModel,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable(Screen.Categories.route) {
                    CategoriesScreen(
                        viewModel = categoriesViewModel,
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
            }
        }
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
