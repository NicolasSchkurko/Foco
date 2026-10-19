package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Los comentarios dicen el porqué en pocas líneas: un bloque de más de [BUDGET] líneas bajo `src/` falla
 * y se nombra; lo que valga la pena se muda a `docs/mechanisms/` con un puntero de una línea.
 */
class ShortCommentsTest {

    private val commentLine = Regex("""^\s*(//|/\*|\*|<!--)""")

    /** Bloques de comentario consecutivos; una línea en blanco dentro del bloque no lo corta. */
    private fun blocksIn(file: File): List<String> {
        val lines = file.readLines()
        val blocks = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            if (!commentLine.containsMatchIn(lines[i])) {
                i++
                continue
            }
            val start = i
            var length = 0
            while (i < lines.size) {
                val blankInside = lines[i].isBlank() && i + 1 < lines.size && commentLine.containsMatchIn(lines[i + 1])
                if (commentLine.containsMatchIn(lines[i])) {
                    length++
                } else if (!blankInside) {
                    break
                }
                i++
            }
            if (length > BUDGET) blocks += "${start + 1}: $length lines (max $BUDGET) → ${lines[start].trim()}"
        }
        return blocks
    }

    @Test
    fun commentBlocksStayWithinTheBudget() {
        val sources = File("src")
        assertTrue("expected sources at ${sources.absolutePath}", sources.isDirectory)

        val violations = mutableListOf<String>()
        sources.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") }
            .forEach { file ->
                val relative = file.relativeTo(sources).path.replace('\\', '/')
                blocksIn(file).forEach { violations += "$relative:$it" }
            }

        if (violations.isNotEmpty()) {
            fail("Comment blocks over the budget (${violations.size}):\n" + violations.joinToString("\n"))
        }
    }

    private companion object {
        const val BUDGET = 4
    }
}
