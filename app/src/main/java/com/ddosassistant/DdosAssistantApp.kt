package com.ddosassistant

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ddosassistant.ui.screens.IncidentDetailScreen
import com.ddosassistant.ui.screens.IncidentsScreen
import com.ddosassistant.ui.screens.SettingsScreen
import com.ddosassistant.ui.screens.viewmodel.IncidentDetailViewModel
import com.ddosassistant.ui.screens.viewmodel.IncidentsViewModel
import com.ddosassistant.ui.screens.viewmodel.SettingsViewModel

@Composable
fun DdosAssistantApp() {
    val navController = rememberNavController()

    val factory = remember { ViewModelFactory() }

    NavHost(navController = navController, startDestination = "incidents") {
        composable("incidents") {
            val vm: IncidentsViewModel = viewModel(factory = factory)
            IncidentsScreen(
                viewModel = vm,
                onOpenIncident = { id -> navController.navigate("incident/$id") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable(
            route = "incident/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            val vm: IncidentDetailViewModel = viewModel(factory = factory)
            vm.bind(id)
            IncidentDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            val vm: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
