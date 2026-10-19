package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The base resources are English and Spanish lives in `es`: a device whose language has no resources of
 * its own (French, German...) falls back to the base and sees English, never Spanish.
 */
class DefaultLocaleResourcesTest {

    private val baseFile = File("src/main/res/values/strings.xml")
    private val spanishFile = File("src/main/res/values-es/strings.xml")

    private fun valuesIn(file: File): Map<String, String> {
        assertTrue("expected $file", file.isFile)
        return Regex("""<string name="([^"]+)">([^<]*)</string>""")
            .findAll(file.readText())
            .associate { it.groupValues[1] to it.groupValues[2] }
    }

    private fun keysIn(file: File): Set<String> {
        assertTrue("expected $file", file.isFile)
        val text = file.readText()
        val strings = Regex("""<string name="([^"]+)"""")
            .findAll(text).map { it.groupValues[1] }
        val plurals = Regex("""<plurals name="([^"]+)"""")
            .findAll(text).map { it.groupValues[1] }
        return (strings + plurals).toSet()
    }

    @Test
    fun theBaseIsEnglishAndSpanishLivesInItsOwnLocale() {
        val base = valuesIn(baseFile)
        val spanish = valuesIn(spanishFile)

        assertEquals("the base must be English", "Dark", base["settings_option_dark"])
        assertEquals("the base must be English", "Settings", base["settings_title"])
        assertEquals("the base must be English", "Today", base["dashboard_title"])

        assertEquals("Español must resolve from the es resources", "Oscuro", spanish["settings_option_dark"])
        assertEquals("Español must resolve from the es resources", "Ajustes", spanish["settings_title"])
        assertEquals("Español must resolve from the es resources", "Hoy", spanish["dashboard_title"])
    }

    @Test
    fun baseAndSpanishCarryTheSameKeys() {
        val base = keysIn(baseFile)
        val spanish = keysIn(spanishFile)

        assertEquals("keys only in the base: ${base - spanish}", emptySet<String>(), base - spanish)
        assertEquals("keys only in es: ${spanish - base}", emptySet<String>(), spanish - base)
    }
}
