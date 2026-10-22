package com.foco.app.structure

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Instrumented tests wait for the state they need; a fixed sleep hides the reason and costs the
 * whole duration even when the state is already there. A bounded poll (a deadline loop) may sleep.
 */
class NoFixedSleepTest {
    /** The sleep calls a test can block on; each one waits a fixed time. */
    private val sleepCalls =
        listOf(
            Regex("""\bThread\s*\.\s*sleep\s*\("""),
            Regex("""\bSystemClock\s*\.\s*sleep\s*\("""),
            Regex("""\bTimeUnit\s*\.\s*\w+\s*\.\s*sleep\s*\("""),
        )

    @Test
    fun noFixedSleepInTheTestSources() {
        val sources = File("src/androidTest")
        assertTrue("expected sources at ${sources.absolutePath}", sources.isDirectory)

        val offenders =
            listOf(File("src/androidTest"), File("src/test"))
                .flatMap { root -> root.walkTopDown().toList() }
                .filter { it.isFile && it.extension == "kt" }
                .flatMap { file ->
                    fixedSleepsIn(file)
                        .filter { (_, bounded) -> !bounded }
                        .map { (line, _) -> "${file.path}:$line" }
                }.toList()

        if (offenders.isNotEmpty()) {
            fail(
                "fixed sleep in a test; wait for the expected state instead:\n" +
                    offenders.joinToString("\n"),
            )
        }
    }

    /** The fixed sleeps of [file]: the line and whether the call sits inside a bounded poll. */
    private fun fixedSleepsIn(file: File): List<Pair<Int, Boolean>> {
        val lines = codeLines(file.readText())
        val sleeps = mutableListOf<Pair<Int, Boolean>>()
        lines.forEachIndexed { index, line ->
            if (!sleepCalls.any { it.containsMatchIn(line) }) return@forEachIndexed
            sleeps += (index + 1) to isInsideADeadlinePoll(lines, index)
        }
        return sleeps
    }

    /** Whether the line at [index] sits inside a loop whose header names the deadline that ends it. */
    private fun isInsideADeadlinePoll(
        lines: List<String>,
        index: Int,
    ): Boolean {
        val indent = indentOf(lines[index])
        val loop =
            (0..index).lastOrNull { line ->
                val text = lines[line].trim()
                text.startsWith("while") &&
                    text.contains("deadline", ignoreCase = true) &&
                    indentOf(lines[line]) < indent
            }
        return loop != null
    }

    /** The column where the line starts, or -1 when it holds nothing but blanks. */
    private fun indentOf(line: String): Int = line.indexOfFirst { !it.isWhitespace() }

    /** The lines of [text] with every string and char literal blanked: a sleep in one is not a call. */
    private fun codeLines(text: String): List<String> {
        val code = StringBuilder()
        var index = 0
        while (index < text.length) {
            val char = text[index]
            if (text.startsWith("\"\"\"", index)) {
                val end = text.indexOf("\"\"\"", index + 3)
                val stop = if (end < 0) text.length else end + 3
                code.append(blank(text.substring(index, stop)))
                index = stop
                continue
            }
            if (char == '"' || char == '\'') {
                val stop = skipLiteral(text, index)
                code.append(blank(text.substring(index, stop)))
                index = stop
                continue
            }
            code.append(char)
            index++
        }
        return code.toString().lines()
    }

    /** The index right after the string or char literal that starts at [index]. */
    private fun skipLiteral(
        text: String,
        index: Int,
    ): Int {
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

    /** [text] with every character but the line breaks replaced by a space. */
    private fun blank(text: String): String = text.map { if (it == '\n') '\n' else ' ' }.joinToString("")

    /** The fixture proves the scan reports every fixed sleep, and only the bounded poll passes. */
    @Test
    fun theScanReportsTheFixedSleeps() {
        val fixtureDir = createTempDirectory(prefix = "sleep")
        try {
            val bad = File(fixtureDir.toFile(), "bad.kt")
            bad.writeText(
                """
                package test
                class T {
                  fun a() { Thread.sleep(100) }
                  fun b() { SystemClock.sleep(50) }
                  fun c() { TimeUnit.SECONDS.sleep(1) }
                  fun d() {
                    val deadline = SystemClock.uptimeMillis() + 15_000
                    while (SystemClock.uptimeMillis() < deadline) {
                      SystemClock.sleep(50)
                    }
                  }
                }
                """.trimIndent(),
            )

            val sleeps = fixedSleepsIn(bad)

            assertTrue("the Thread.sleep must be reported: $sleeps", sleeps.any { it.first == 3 && !it.second })
            assertTrue("the SystemClock.sleep must be reported: $sleeps", sleeps.any { it.first == 4 && !it.second })
            assertTrue("the TimeUnit sleep must be reported: $sleeps", sleeps.any { it.first == 5 && !it.second })
            assertTrue("a bounded poll may sleep: $sleeps", sleeps.any { it.first == 9 && it.second })
        } finally {
            fixtureDir.toFile().deleteRecursively()
        }
    }
}
