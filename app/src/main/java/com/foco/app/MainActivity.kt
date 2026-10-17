package com.foco.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.foco.app.data.AppLanguage
import com.foco.app.data.SettingsStore
import com.foco.app.model.AppearanceOption
import com.foco.app.ui.App
import com.foco.app.ui.theme.FocoTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Punto de entrada: aplica el idioma guardado antes del primer frame y monta el
 * árbol Compose con la apariencia elegida (Automático = tema del sistema).
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settings = SettingsStore(applicationContext)
        // Lectura puntual del DataStore en el arranque: evita el parpadeo/recreado
        // que causaría aplicar el idioma después del primer frame.
        runBlocking { AppLanguage.applyToSystem(settings.language.first()) }

        enableEdgeToEdge()
        setContent {
            val appearance by settings.appearance.collectAsState(initial = AppearanceOption.AUTO)
            FocoTheme(darkTheme = appearance.isDark(isSystemInDarkTheme())) {
                App()
            }
        }
    }
}
