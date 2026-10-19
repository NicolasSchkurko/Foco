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
import androidx.lifecycle.lifecycleScope
import com.foco.app.data.AppAppearance
import com.foco.app.data.AppLanguage
import com.foco.app.data.SessionHistoryStore
import com.foco.app.data.SettingsStore
import com.foco.app.data.UsageRepository
import com.foco.app.data.migrateSettingsFromLegacyDataStore
import com.foco.app.model.AppearanceOption
import com.foco.app.ui.App
import com.foco.app.ui.theme.FocoTheme
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
        consolidateHistory()
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

    /**
     * Best-effort backfill: stores the finished days on IO, once per app process. A failure is not fatal —
     * the next start retries, and today is read live from the platform anyway.
     */
    private fun consolidateHistory() {
        if (!consolidating.compareAndSet(false, true)) return
        val repository = UsageRepository(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                SessionHistoryStore.forApp(applicationContext) { date -> repository.loadDay(date) }
                    .consolidate(LocalDate.now())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                // The backfill retries on the next start; nothing here is user-facing.
            } finally {
                consolidating.set(false)
            }
        }
    }

    private companion object {
        val consolidating = AtomicBoolean(false)
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
