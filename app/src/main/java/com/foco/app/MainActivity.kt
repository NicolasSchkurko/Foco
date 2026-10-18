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
import com.foco.app.data.SettingsStore
import com.foco.app.data.migrateSettingsFromLegacyDataStore
import com.foco.app.model.AppearanceOption
import com.foco.app.ui.App
import com.foco.app.ui.theme.FocoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Punto de entrada: migra las preferencias del store legado si hace falta, aplica el
 * idioma y la apariencia guardados antes del primer frame (el tema de la activity y
 * el fondo de la ventana siguen la elección, no solo Compose) y monta el árbol Compose.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsStore(applicationContext)
        // Lectura puntual del DataStore en el arranque: la migración corre primero
        // (una sola vez), el idioma se aplica y la apariencia se lleva al tema antes
        // del primer frame. El valor leído arranca el estado de Compose.
        val storedAppearance = runBlocking {
            migrateSettingsFromLegacyDataStore(applicationContext)
            AppLanguage.applyToSystem(settings.language.first())
            settings.appearance.first().also { AppAppearance.applyToSystem(it) }
        }

        enableEdgeToEdge()
        setContent {
            val appearance by settings.appearance.collectAsState(initial = storedAppearance)
            // Elegir otra apariencia en Ajustes llega hasta acá y se aplica al instante;
            // appcompat recrea la activity si el modo cambia, y el arranque lo vuelve a
            // aplicar. Repetir el modo vigente no hace nada (appcompat lo compara).
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
 * Efecto con nombre: pinta el fondo de la ventana con el `surface` del tema Compose
 * (colores dinámicos incluidos), así una transición o la recreación por cambio de
 * idioma no descubre el esquema contrario. Recurso físico: el `Window` de la activity,
 * que vive lo que la activity.
 */
@Composable
private fun WindowBackgroundEffect(window: Window, surface: Color) {
    SideEffect {
        window.setBackgroundDrawable(ColorDrawable(surface.toArgb()))
    }
}
