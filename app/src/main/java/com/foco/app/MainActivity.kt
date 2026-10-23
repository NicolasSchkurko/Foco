package com.foco.app

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.foco.app.data.consolidation.ConsolidationScheduler
import com.foco.app.data.monitor.UsageMonitorService
import com.foco.app.data.stores.SettingsStore
import com.foco.app.data.stores.migrateSettingsFromLegacyDataStore
import com.foco.app.data.system.AppAppearance
import com.foco.app.data.system.AppLanguage
import com.foco.app.domain.consolidation.withStartupTimeout
import com.foco.app.model.AppearanceOption
import com.foco.app.ui.App
import com.foco.app.ui.theme.FocoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val STARTUP_TIMEOUT_MILLIS = 2_000L
private const val TAG = "MainActivity"

/**
 * Entry point: migrates the legacy store, applies language and appearance before the first frame (the theme and
 * the window backdrop follow the choice) and mounts Compose.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsStore(applicationContext)
        val storedAppearance = loadTheStartupSettings(settings)

        enableEdgeToEdge()
        scheduleTheNightlyConsolidation()
        setContent {
            val appearance by settings.appearance.collectAsState(initial = storedAppearance)

            LaunchedEffect(appearance) { AppAppearance.applyToSystem(appearance) }

            FocoTheme(darkTheme = appearance.isDark(isSystemInDarkTheme())) {
                WindowBackgroundEffect(
                    window = window,
                    surface = MaterialTheme.colorScheme.surface,
                )
                App()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncTheLimitMonitor()
    }

    /**
     * The bounded startup read: the legacy store is migrated, the language is applied and the
     * appearance reaches the theme before the first frame.
     */
    private fun loadTheStartupSettings(settings: SettingsStore): AppearanceOption =
        runBlocking {
            withStartupTimeout(
                timeoutMillis = STARTUP_TIMEOUT_MILLIS,
                fallback = {
                    Log.w(TAG, "Settings startup timed out or failed; using defaults")
                    AppearanceOption.AUTO
                },
            ) {
                migrateSettingsFromLegacyDataStore(applicationContext)
                applyTheLanguage(settings)
                applyTheAppearance(settings)
            }
        }

    /** The system's locale list follows the language the user picked. */
    private suspend fun applyTheLanguage(settings: SettingsStore) {
        AppLanguage.applyToSystem(settings.language.first())
    }

    /** The appearance reaches the theme and the window backdrop; the value read seeds the state. */
    private suspend fun applyTheAppearance(settings: SettingsStore): AppearanceOption =
        settings.appearance.first().also { AppAppearance.applyToSystem(it) }

    /** The nightly consolidation job; WorkManager keeps it across the starts. */
    private fun scheduleTheNightlyConsolidation() {
        ConsolidationScheduler.schedule(applicationContext)
    }

    /** The monitor starts only with a limit and accessibility; this also covers enabling
     *  accessibility in Settings and coming back. */
    private fun syncTheLimitMonitor() {
        UsageMonitorService.sync(applicationContext)
    }
}

/**
 * Named effect: paints the window backdrop with the theme's `surface`, so a transition or the
 * re-creation never reveals the opposite scheme. Resource: the activity's `Window`.
 */
@Composable
private fun WindowBackgroundEffect(
    window: Window,
    surface: Color,
) {
    SideEffect {
        window.setBackgroundDrawable(ColorDrawable(surface.toArgb()))
    }
}
