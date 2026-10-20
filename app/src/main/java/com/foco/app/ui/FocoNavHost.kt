package com.foco.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.foco.app.model.day.DayData
import com.foco.app.ui.dashboard.DashboardScreen
import com.foco.app.ui.detail.AppDetailScreen
import com.foco.app.ui.settings.SettingsScreen
import java.time.LocalDate

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
                onAppClick = { packageName, date ->
                    val route =
                        if (date == null) {
                            "detail/$packageName"
                        } else {
                            "detail/$packageName?date=$date"
                        }
                    navController.navigate(route)
                },
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
            AppDetailScreen(
                packageName = backStackEntry.arguments?.getString("packageName").orEmpty(),
                date =
                    backStackEntry.arguments
                        ?.getString("date")
                        ?.takeIf { it.isNotEmpty() }
                        ?.let(LocalDate::parse),
            )
        }
    }
}
