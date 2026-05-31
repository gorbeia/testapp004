package com.example.testapp004.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.testapp004.ui.screens.AcquaintanceDetailScreen
import com.example.testapp004.ui.screens.AcquaintancesListScreen
import com.example.testapp004.ui.screens.AddEditAcquaintanceScreen
import com.example.testapp004.ui.screens.CategoriesScreen
import com.example.testapp004.viewmodel.AcquaintanceDetailViewModel
import com.example.testapp004.viewmodel.AcquaintancesViewModel
import com.example.testapp004.viewmodel.AddEditAcquaintanceViewModel
import com.example.testapp004.viewmodel.CategoriesViewModel

sealed class Screen(val route: String) {
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

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val acquaintancesViewModel: AcquaintancesViewModel = hiltViewModel()
    val categoriesViewModel: CategoriesViewModel = hiltViewModel()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.AcquaintancesList.route,
            modifier = Modifier.padding(innerPadding),
        ) {
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
