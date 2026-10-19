package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Source prose is English: a comment line under `src/` carrying a Spanish marker fails and is named.
 * String values and test selectors that target the Spanish UI are data, not prose, so they are not read.
 */
class SourceProseTest {

    private val commentLine = Regex("""^\s*(//|/\*|\*|<!--)""")

    private val markers = Regex(
        "[áéíóúüñ¿¡«»]|\\b(el|la|los|las|una|unos|unas|del|al|que|para|como|cuando|desde|hasta|más|sólo|" +
            "también|hay|tiene|tienen|está|están|sus|pero|porque|donde|todo|toda|otro|otra|mismo|misma|cada|" +
            "vez|día|días|apariencia|idioma|ajustes|oscuro|claro|usuario|pantalla|archivo|sesión|datos|" +
            "sólo|así|acá|después|antes|nunca|siempre|sólo)\\b",
        RegexOption.IGNORE_CASE
    )

    @Test
    fun sourceCommentsAreEnglish() {
        val sources = File("src")
        assertTrue("expected sources at ${sources.absolutePath}", sources.isDirectory)

        val violations = mutableListOf<String>()
        sources.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") }
            .forEach { file ->
                val relative = file.relativeTo(sources).path.replace('\\', '/')
                file.readLines().forEachIndexed { index, line ->
                    if (!commentLine.containsMatchIn(line)) return@forEachIndexed
                    markers.find(line)?.let {
                        violations += "$relative:${index + 1}: '${it.value}' → ${line.trim()}"
                    }
                }
            }

        if (violations.isNotEmpty()) {
            fail("Spanish prose in comments (${violations.size}):\n" + violations.joinToString("\n"))
        }
    }
}
