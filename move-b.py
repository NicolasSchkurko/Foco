import io

# --- 1) AppDetailScreen: 30% threshold (proposal) + remove unused Settings import ---
p = 'app/src/main/java/com/foco/app/ui/detail/AppDetailScreen.kt'
t = io.open(p, encoding='utf-8').read()
t = t.replace('if (dx > size.width * 0.25f) {', 'if (dx > size.width * 0.3f) {', 1)
t = t.replace('import androidx.compose.material.icons.filled.Settings\n', '', 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('detail: threshold 30%, Settings import removed')

# --- 2) DashboardScreen: drop the added alert icon + onOpenLimits ---
p = 'app/src/main/java/com/foco/app/ui/dashboard/DashboardScreen.kt'
t = io.open(p, encoding='utf-8').read()
old_btn = '''                    IconButton(onClick = onOpenLimits) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = stringResource(R.string.detail_limits_section),
                        )
                    }
'''
assert old_btn in t, 'dashboard icon not found'
t = t.replace(old_btn, '', 1)
t = t.replace('    onOpenLimits: () -> Unit = {},\n', '', 1)
t = t.replace('import androidx.compose.material.icons.filled.Notifications\n', '', 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('dashboard: alert icon removed')

# --- 3) FocoNavHost: back to the plain settings route ---
p = 'app/src/main/java/com/foco/app/ui/FocoNavHost.kt'
t = io.open(p, encoding='utf-8').read()
t = t.replace('                onOpenLimits = { navController.navigate("settings?focusLimits=true") },\n', '', 1)
old_route = '''        composable(
            route = "settings?focusLimits={focusLimits}",
            arguments =
                listOf(
                    navArgument("focusLimits") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
        ) { backStackEntry ->
            SettingsScreen(focusLimits = backStackEntry.arguments?.getBoolean("focusLimits") ?: false)
        }'''
new_route = '''        composable("settings") {
            SettingsScreen()
        }'''
assert old_route in t, 'route not found'
t = t.replace(old_route, new_route, 1)
io.open(p, 'w', encoding='utf-8').write(t)
print('navhost: plain settings route')
