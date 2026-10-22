package com.foco.app.structure

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Source prose is English: a comment block, a trailing comment or a test failure message with a
 * Spanish marker fails and is named. A string value is data, not prose, and never matches.
 */
class SourceProseTest {
    /** The Spanish words that only live in prose; a value inside a string never reaches them. */
    private val spanishMarker =
        Regex(
            "[áéíóúüñ¿¡«»]|\\b(el|la|los|las|una|unos|unas|del|al|que|para|como|cuando|desde|hasta|más|sólo|" +
                "también|hay|tiene|tienen|está|están|sus|pero|porque|donde|todo|toda|otro|otra|mismo|misma|cada|" +
                "vez|día|días|apariencia|idioma|ajustes|oscuro|claro|usuario|pantalla|archivo|sesión|datos|" +
                "así|acá|después|antes|nunca|siempre|barras|hoy|fuera|propósito|descarta|crece|permiso)\\b",
            RegexOption.IGNORE_CASE,
        )

    /** A piece of prose: its text and the line it starts on. */
    private data class Span(
        val line: Int,
        val text: String,
    )

    @Test
    fun sourceCommentsAreEnglish() {
        val sources = File("src")
        assertTrue("expected sources at ${sources.absolutePath}", sources.isDirectory)

        val violations = commentViolationsIn(sources)
        if (violations.isNotEmpty()) {
            fail("Spanish prose in comments (${violations.size}):\n" + violations.joinToString("\n"))
        }
    }

    @Test
    fun testFailureMessagesAreEnglish() {
        val tests = File("src/test")
        assertTrue("expected tests at ${tests.absolutePath}", tests.isDirectory)

        val violations = messageViolationsIn(tests) + messageViolationsIn(File("src/androidTest"))
        if (violations.isNotEmpty()) {
            fail("Spanish prose in test failure messages (${violations.size}):\n" + violations.joinToString("\n"))
        }
    }

    /** The comment violations under [root]: each names the file, the line and the marker. */
    private fun commentViolationsIn(root: File): List<String> {
        val violations = mutableListOf<String>()
        root
            .walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") }
            .forEach { file ->
                val text = file.readText()
                commentSpans(text, xml = file.extension == "xml").forEach { span ->
                    val marker = spanishMarker.find(span.text) ?: return@forEach
                    val start = marker.range.first
                    val before = span.text.substring(0, start)
                    val offset = before.count { it == '\n' }
                    val line = span.line + offset
                    val lines = span.text.lines()
                    val found = lines.getOrNull(offset)
                    val offending = found?.trim() ?: span.text.trim()
                    violations += "${file.path}:$line: '${marker.value}' → $offending"
                }
            }
        return violations
    }

    /** The failure-message violations under [root]: each names the file, the line and the marker. */
    private fun messageViolationsIn(root: File): List<String> {
        val violations = mutableListOf<String>()
        root
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val text = file.readText()
                failureMessages(text).forEach { span ->
                    val marker = spanishMarker.find(span.text) ?: return@forEach
                    violations += "${file.path}:${span.line}: '${marker.value}' → ${span.text}"
                }
            }
        return violations
    }

    /**
     * The comment spans of [text]: whole-line and trailing line comments, block comments (KDoc
     * included) and XML comments. String and char literals are skipped: a value is data.
     */
    private fun commentSpans(
        text: String,
        xml: Boolean,
    ): List<Span> {
        val spans = mutableListOf<Span>()
        var index = 0
        var line = 1
        while (index < text.length) {
            val char = text[index]
            if (char == '\n') {
                line++
                index++
                continue
            }
            if (xml) {
                if (!text.startsWith("<!--", index)) {
                    index++
                    continue
                }
                val end = text.indexOf("-->", index)
                val stop = if (end < 0) text.length else end + 3
                val block = text.substring(index, stop)
                spans += Span(line, block)
                line += block.count { it == '\n' }
                index = stop
                continue
            }
            when {
                text.startsWith("//", index) -> {
                    val end = text.indexOf('\n', index).let { if (it < 0) text.length else it }
                    spans += Span(line, text.substring(index, end))
                    index = end
                }

                text.startsWith("/*", index) -> {
                    val end = text.indexOf("*/", index + 2)
                    val stop = if (end < 0) text.length else end + 2
                    val block = text.substring(index, stop)
                    spans += Span(line, block)
                    line += block.count { it == '\n' }
                    index = stop
                }

                char == '"' || char == '\'' -> {
                    val stop = skipLiteral(text, index)
                    line += text.substring(index, stop).count { it == '\n' }
                    index = stop
                }

                else -> index++
            }
        }
        return spans
    }

    /** The index right after the string or char literal that starts at [index]. */
    private fun skipLiteral(
        text: String,
        index: Int,
    ): Int {
        if (text.startsWith("\"\"\"", index)) {
            val end = text.indexOf("\"\"\"", index + 3)
            return if (end < 0) text.length else end + 3
        }
        val quote = text[index]
        var cursor = index + 1
        while (cursor < text.length) {
            when (text[cursor]) {
                '\\' -> cursor += 2
                '\n' -> return cursor
                quote -> return cursor + 1
                else -> cursor++
            }
        }
        return text.length
    }

    /** The failure messages of [text]: the message argument of every assertion call. */
    private fun failureMessages(text: String): List<Span> {
        val messages = mutableListOf<Span>()
        val masked = mask(text)
        val call =
            Regex("""\b(assertEquals|assertTrue|assertFalse|assertNull|assertNotNull|assertSame|fail)\s*\(""")
        call.findAll(masked.text).forEach { match ->
            if (masked.inside[match.range.first]) return@forEach
            val open = match.range.last
            val arguments = argumentsOf(text, open) ?: return@forEach
            val first = arguments.firstOrNull() ?: return@forEach
            val isMessage = if (match.groupValues[1] == "assertEquals") arguments.size >= 3 else true
            if (!isMessage) return@forEach
            val literal = stringLiteral(first) ?: return@forEach
            messages += Span(text.substring(0, open).count { it == '\n' } + 1, literal)
        }
        return messages
    }

    /**
     * [text] with the content of every literal blanked, and the offsets that sit inside a literal:
     * an assertion written inside a string is text, not a call.
     */
    private fun mask(text: String): Masked {
        val masked = StringBuilder(text)
        val inside = BooleanArray(text.length)
        var index = 0
        while (index < text.length) {
            val stop: Int
            val contentStart: Int
            val contentEnd: Int
            if (text.startsWith("\"\"\"", index)) {
                val end = text.indexOf("\"\"\"", index + 3)
                stop = if (end < 0) text.length else end + 3
                contentStart = index + 3
                contentEnd = stop - 1
            } else if (text[index] == '"' || text[index] == '\'') {
                stop = skipLiteral(text, index)
                contentStart = index + 1
                contentEnd = stop - 1
            } else {
                index++
                continue
            }
            for (cursor in contentStart until contentEnd) {
                inside[cursor] = true
                if (masked[cursor] != '\n') masked.setCharAt(cursor, ' ')
            }
            index = stop
        }
        return Masked(masked.toString(), inside)
    }

    /** The masked text and the offsets that sit inside a literal. */
    private class Masked(
        val text: String,
        val inside: BooleanArray,
    )

    /** The arguments of the call whose parenthesis opens at [open]; null when it never closes. */
    private fun argumentsOf(
        text: String,
        open: Int,
    ): List<String>? {
        if (open >= text.length || text[open] != '(') return null
        val arguments = mutableListOf<String>()
        val start = open + 1
        var depth = 0
        var cursor = start
        var argumentStart = start
        while (cursor < text.length) {
            when {
                text.startsWith("\"\"\"", cursor) -> cursor = skipLiteral(text, cursor)
                text[cursor] == '"' || text[cursor] == '\'' -> cursor = skipLiteral(text, cursor)
                text[cursor] == '(' || text[cursor] == '[' -> {
                    depth++
                    cursor++
                }

                text[cursor] == ')' && depth == 0 -> {
                    arguments += text.substring(argumentStart, cursor).trim()
                    return arguments
                }

                text[cursor] == ')' || text[cursor] == ']' -> {
                    depth--
                    cursor++
                }

                text[cursor] == ',' && depth == 0 -> {
                    arguments += text.substring(argumentStart, cursor).trim()
                    argumentStart = cursor + 1
                    cursor++
                }

                else -> cursor++
            }
        }
        return null
    }

    /** The text of [argument] when it is a plain string literal; null otherwise. */
    private fun stringLiteral(argument: String): String? {
        val trimmed = argument.trim()
        if (!trimmed.startsWith("\"") || trimmed.length < 2 || !trimmed.endsWith("\"")) return null
        val content = trimmed.substring(1, trimmed.length - 1)
        var cursor = 0
        while (cursor < content.length) {
            when (content[cursor]) {
                '\\' -> cursor += 2
                '"' -> return null
                else -> cursor++
            }
        }
        return content
    }

    /** The fixture proves the scan reads the whole block, the trailing comments and the messages. */
    @Test
    fun theScanReportsTheSpanishOfABlockTheSecondLineAndATrailingComment() {
        val fixtureDir = createTempDirectory(prefix = "prose")
        try {
            File(fixtureDir.toFile(), "bad.kt")
                .writeText(
                    """
                    package test
                    class T {
                      // the first line of the block is English
                      // la segunda linea del bloque esta en espanol
                      val label = "Ajustes"
                      fun x(): String = label // el comentario final tambien se lee
                    }
                    """.trimIndent(),
                )
            File(fixtureDir.toFile(), "bad.xml")
                .writeText(
                    """
                    <?xml version="1.0" encoding="utf-8"?>
                    <resources>
                      <!--
                        The first line of the block is English.
                        El texto de la aplicacion esta en espanol.
                      -->
                      <string name="label">Ajustes</string>
                    </resources>
                    """.trimIndent(),
                )

            val violations = commentViolationsIn(fixtureDir.toFile())

            assertTrue("the second line of a block must be read: $violations", violations.any { it.contains("la segunda linea") })
            assertTrue("a trailing comment must be read: $violations", violations.any { it.contains("el comentario final") })
            assertTrue(
                "the second line of an XML block must be read: $violations",
                violations.any { it.contains("El texto de la aplicacion") },
            )
            assertTrue("a string value is data, not prose: $violations", violations.none { it.contains("Ajustes") })
        } finally {
            fixtureDir.toFile().deleteRecursively()
        }
    }

    /** The fixture proves the failure-message branch of the scan reports a Spanish message. */
    @Test
    fun theScanReportsASpanishFailureMessage() {
        val fixtureDir = createTempDirectory(prefix = "prose-message")
        try {
            File(fixtureDir.toFile(), "bad.kt")
                .writeText(
                    """
                    package test
                    class T {
                      fun x() { assertEquals("el codigo esta roto", 1, 2) }
                    }
                    """.trimIndent(),
                )

            val violations = messageViolationsIn(fixtureDir.toFile())

            assertTrue("the scan must report the Spanish message: $violations", violations.any { it.contains("el codigo esta roto") })
            assertTrue("the violation must name the file: $violations", violations.any { it.contains("bad.kt") })
        } finally {
            fixtureDir.toFile().deleteRecursively()
        }
    }
}
