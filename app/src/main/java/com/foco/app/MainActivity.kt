package com.foco.app

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
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
import com.foco.app.data.AppAppearance
import com.foco.app.data.AppLanguage
import com.foco.app.data.ConsolidationScheduler
import com.foco.app.data.SettingsStore
import com.foco.app.data.migrateSettingsFromLegacyDataStore
import com.foco.app.model.AppearanceOption
import com.foco.app.ui.App
import com.foco.app.ui.theme.FocoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Entry point: migrates the legacy store, applies language and appearance before the first frame (the theme and
 * the window backdrop follow the choice) and mounts Compose.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsStore(applicationContext)
        // One-shot DataStore read at startup: the migration runs first
        // (once), the language is applied and the appearance reaches the theme before
        // the first frame. The value read seeds the Compose state.
        val storedAppearance = runBlocking {
            migrateSettingsFromLegacyDataStore(applicationContext)
            AppLanguage.applyToSystem(settings.language.first())
            settings.appearance.first().also { AppAppearance.applyToSystem(it) }
        }

        enableEdgeToEdge()
        ConsolidationScheduler.schedule(applicationContext)
        setContent {
            val appearance by settings.appearance.collectAsState(initial = storedAppearance)
            // Picking another appearance in Settings reaches here and applies at once;
            // appcompat re-creates the activity if the mode changes, and startup applies it
            // again. Repeating the mode in force does nothing (appcompat compares it).
            LaunchedEffect(appearance) { AppAppearance.applyToSystem(appearance) }

            FocoTheme(darkTheme = appearance.isDark(isSystemInDarkTheme())) {
                WindowBackgroundEffect(
                    window = window,
                    surface = MaterialTheme.colorScheme.surface
                )
                App()
            }
        }
    }

}

/**
 * Named effect: paints the window backdrop with the theme's `surface`, so a transition or the
 * re-creation never reveals the opposite scheme. Resource: the activity's `Window`.
 */
@Composable
private fun WindowBackgroundEffect(window: Window, surface: Color) {
    SideEffect {
        window.setBackgroundDrawable(ColorDrawable(surface.toArgb()))
    }
}
