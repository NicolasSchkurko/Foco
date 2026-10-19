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
import com.foco.app.ui.week.WeekScreen
import java.time.LocalDate

/**
 * The app's routes: the dashboard, the week, a dated day view and the detail, optionally on a date.
 * The permission gate stays outside, in `App`; `loadDay` is the tests' seam for the live day's read.
 */
@Composable
fun FocoNavHost(loadDay: (suspend () -> DayData)? = null) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                loadDay = loadDay,
                onAppClick = { packageName -> navController.navigate("detail/$packageName") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenWeek = { navController.navigate("week") }
            )
        }

        composable("week") {
            WeekScreen(
                loadDay = loadDay,
                onDayClick = { date -> navController.navigate("day/$date") },
                onAppClick = { packageName -> navController.navigate("detail/$packageName") }
            )
        }

        composable("settings") {
            SettingsScreen()
        }

        composable(
            route = "day/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date").orEmpty()
            DashboardScreen(
                date = LocalDate.parse(date),
                loadDay = loadDay,
                onAppClick = { packageName -> navController.navigate("detail/$packageName?date=$date") }
            )
        }

        composable(
            route = "detail/{packageName}?date={date}",
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            AppDetailScreen(
                packageName = backStackEntry.arguments?.getString("packageName").orEmpty(),
                date = backStackEntry.arguments?.getString("date")
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(LocalDate::parse)
            )
        }
    }
}
