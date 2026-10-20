package com.foco.app.data

import androidx.appcompat.app.AppCompatDelegate
import com.foco.app.model.AppearanceOption
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The appearance -> appcompat night mode mapping; the constants are `static final`, so the
 * assertion runs on the JVM without the framework (`applyToSystem` cannot be called: `android.jar` is a stub).
 */
class AppAppearanceTest {
    @Test
    fun `automatic follows the system night mode`() {
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppAppearance.nightModeFor(AppearanceOption.AUTO),
        )
    }

    @Test
    fun `light and dark override the system night mode`() {
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_NO,
            AppAppearance.nightModeFor(AppearanceOption.LIGHT),
        )
        assertEquals(
            AppCompatDelegate.MODE_NIGHT_YES,
            AppAppearance.nightModeFor(AppearanceOption.DARK),
        )
    }
}
