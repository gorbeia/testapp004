package com.example.testapp004.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.testapp004.ui.screens.HomeScreen
import com.example.testapp004.viewmodel.NotesViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val notesViewModel: NotesViewModel = viewModel()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(viewModel = notesViewModel)
        }
    }
}
