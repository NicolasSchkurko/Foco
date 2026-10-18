package com.foco.app.data

import androidx.appcompat.app.AppCompatDelegate
import com.foco.app.model.AppearanceOption
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * El mapeo apariencia → modo nocturno de appcompat: es lo que lleva la elección al
 * tema de la activity y al fondo de la ventana (no solo al `darkTheme` de Compose).
 * Las constantes de appcompat son `static final`, así que la aserción corre en la JVM
 * sin framework: `applyToSystem` no se puede llamar aquí (el `android.jar` es un stub).
 */
class AppAppearanceTest {

    @Test
    fun `automatic follows the system night mode`() {
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppAppearance.nightModeFor(AppearanceOption.AUTO)
        )
    }

    @Test
    fun `light and dark override the system night mode`() {
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppAppearance.nightModeFor(AppearanceOption.LIGHT)
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            AppAppearance.nightModeFor(AppearanceOption.DARK)
        )
    }
}
