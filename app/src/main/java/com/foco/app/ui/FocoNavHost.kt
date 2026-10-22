package com.foco.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.foco.app.model.day.DayData
import com.foco.app.ui.dashboard.DashboardScreen
import com.foco.app.ui.detail.AppAppSettingsScreen
import com.foco.app.ui.detail.AppDetailScreen
import com.foco.app.ui.settings.SettingsScreen

/**
 * The app's routes: the home, settings and the detail. The home owns the day ring; the week and the
 * dated day are positions in it, not routes. The permission gate stays outside, in `App`.
 */
@Composable
fun FocoNavHost(loadDay: (suspend () -> DayData)? = null) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                loadDay = loadDay,
                onAppClick = { target -> navController.navigate(detailRoute(target)) },
                onOpenSettings = { navController.navigate("settings") },
            )
        }

        composable("settings") {
            SettingsScreen()
        }

        composable(
            route = "detail/{packageName}?date={date}",
            arguments =
                listOf(
                    navArgument("packageName") { type = NavType.StringType },
                    navArgument("date") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName").orEmpty()
            AppDetailScreen(
                packageName = packageName,
                date = parseRouteDate(backStackEntry.arguments?.getString("date")),
                onConfigure = { navController.navigate("app_settings/$packageName") },
            )
        }

        composable(
            route = "app_settings/{packageName}",
            arguments =
                listOf(
                    navArgument("packageName") { type = NavType.StringType },
                ),
        ) { backStackEntry ->
            AppAppSettingsScreen(
                packageName = backStackEntry.arguments?.getString("packageName").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
