package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Source files and top-level docs are UTF-8 without BOM and carry no replacement characters. */
class SourceEncodingTest {

    private val replacement = '\uFFFD'

    @Test
    fun sourcesAndTopLevelDocsAreCleanUtf8() {
        val sourceRoot = File("src")
        assertTrue("expected sources at ${sourceRoot.absolutePath}", sourceRoot.isDirectory)

        val files = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml", "md") }
            .toList() + File("..").listFiles { file ->
            file.isFile && file.extension == "md"
        }.orEmpty()

        val violations = mutableListOf<String>()
        files.forEach { file ->
            val bytes = file.readBytes()
            if (hasBom(bytes)) violations += "${file.path}: UTF-8 BOM"

            file.readLines().forEachIndexed { index, line ->
                if (line.indexOf(replacement) >= 0) {
                    violations += "${file.path}:${index + 1}: U+FFFD"
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail("Source encoding violations:\n" + violations.joinToString("\n"))
        }
    }

    private fun hasBom(bytes: ByteArray): Boolean =
        bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
}
