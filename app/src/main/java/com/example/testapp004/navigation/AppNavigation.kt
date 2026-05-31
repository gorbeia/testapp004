package com.example.testapp004.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.testapp004.ui.screens.EditNoteScreen
import com.example.testapp004.ui.screens.HomeScreen
import com.example.testapp004.viewmodel.NotesViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")

    object EditNote : Screen("edit_note/{noteId}") {
        const val ARG_NOTE_ID = "noteId"

        fun createRoute(noteId: Long) = "edit_note/$noteId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val notesViewModel: NotesViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
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
    }
}
