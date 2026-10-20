package com.foco.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The chosen appearance resolves the final `darkTheme`: Automatic follows the system. */
class AppearanceOptionTest {
    @Test
    fun `auto follows the system`() {
        assertTrue(AppearanceOption.AUTO.isDark(systemInDarkTheme = true))
        assertFalse(AppearanceOption.AUTO.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `light and dark override the system`() {
        assertFalse(AppearanceOption.LIGHT.isDark(systemInDarkTheme = true))
        assertTrue(AppearanceOption.DARK.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `unknown wire values fall back to auto`() {
        assertEquals(AppearanceOption.AUTO, AppearanceOption.fromWire(null))
        assertEquals(AppearanceOption.AUTO, AppearanceOption.fromWire("weird"))
        assertEquals(AppearanceOption.DARK, AppearanceOption.fromWire("dark"))
    }
}
