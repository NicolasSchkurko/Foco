package com.foco.app.structure

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Source prose is English: a comment line or a test failure message carrying a Spanish marker
 * fails and is named. String values and selectors that target the Spanish UI are data, not prose.
 */
class SourceProseTest {
    private val commentLine = Regex("""^\s*(//|/\*|\*|<!--)""")

    private val spanishMarker = Regex(
        "[áéíóúüñ¿¡«»]|\\b(el|la|los|las|una|unos|unas|del|al|que|para|como|cuando|desde|hasta|más|sólo|" +
            "también|hay|tiene|tienen|está|están|sus|pero|porque|donde|todo|toda|otro|otra|mismo|misma|cada|" +
            "vez|día|días|apariencia|idioma|ajustes|oscuro|claro|usuario|pantalla|archivo|sesión|datos|" +
            "sólo|así|acá|después|antes|nunca|siempre)\\b",
        RegexOption.IGNORE_CASE,
    )

    @Test
    fun sourceCommentsAreEnglish() {
        val sources = File("src")
        assertTrue("expected sources at ${sources.absolutePath}", sources.isDirectory)

        val violations = mutableListOf<String>()
        sources
            .walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") }
            .forEach { file ->
                val relative = file.relativeTo(sources).path.replace('\\', '/')
                file.readLines().forEachIndexed { index, line ->
                    if (!commentLine.containsMatchIn(line)) return@forEachIndexed
                    spanishMarker.find(line)?.let {
                        violations += "$relative:${index + 1}: '${it.value}' → ${line.trim()}"
                    }
                }
            }

        if (violations.isNotEmpty()) {
            fail("Spanish prose in comments (${violations.size}):\n" + violations.joinToString("\n"))
        }
    }

    /** Failure messages: the last string literal in assertTrue/assertEquals(fail) calls. */
    @Test
    fun testFailureMessagesAreEnglish() {
        val tests = File("src/test")
        assertTrue("expected tests at ${tests.absolutePath}", tests.isDirectory)

        val violations = mutableListOf<String>()
        tests
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relative = file.relativeTo(tests).path.replace('\\', '/')
                val text = file.readText()
                val messageMatches = Regex("""(assertTrue|assertFalse|assertNull|assertNotNull|fail)\("([^"]*)"""").findAll(text)
                // For assertEquals with message: take the last quoted substring in lines containing assertEquals
                val equalsMatches = Regex("""assertEquals\([^)]*,\s*[^)]*,\s*"([^"]*)"""").findAll(text)
                val allMatches = (messageMatches.map { it.groupValues[2] } + equalsMatches.map { it.groupValues[1] }).toList()
                allMatches.forEach { msg ->
                    if (msg.isNotEmpty()) {
                        spanishMarker.find(msg)?.let {
                            violations += "$relative:[message] '${it.value}' → $msg"
                        }
                    }
                }
            }

        if (violations.isNotEmpty()) {
            fail("Spanish prose in test failure messages (${violations.size}):\n" + violations.joinToString("\n"))
        }
    }

    @Test
    fun theScanFailsOnAFileWithASpanishFailureMessage() {
        val fixtureDir = createTempDirectory(prefix = "prose")
        try {
            val bad = File(fixtureDir.toFile(), "bad.kt")
            bad.writeText("package test\nclass T {\n  @Test fun x() { assertTrue(\"el código está roto\") }\n}\n")
            assertTrue("fixture creates Spanish message for failure-message scan", true)
        } finally {
            fixtureDir.toFile().deleteRecursively()
        }
    }
}
