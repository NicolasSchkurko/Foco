package com.foco.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.foco.app.ui.dashboard.AppDetailScreen
import com.foco.app.ui.dashboard.DashboardScreen

/** Rutas de la app; el gate de permiso queda afuera, en `App`. */
@Composable
fun FocoNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onAppClick = { packageName -> navController.navigate("detail/$packageName") }
            )
        }

        composable(
            route = "detail/{packageName}",
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { backStackEntry ->
            AppDetailScreen(
                packageName = backStackEntry.arguments?.getString("packageName").orEmpty()
            )
        }
    }
}
