package com.foco.app.structure

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/** Source files and every tracked Markdown are UTF-8 without BOM and carry no replacement characters. */
class SourceEncodingTest {
    private val replacement = String(Character.toChars(0xFFFD))

    /** The files the rule covers: the sources and every Markdown file of the repository. */
    private fun coveredFiles(root: File): List<File> {
        val sources =
            root
                .walkTopDown()
                .filter { it.isFile && it.extension in setOf("kt", "xml") &&
                    !it.path.contains("build/") &&
                    !it.path.contains("SourceEncodingTest")
                }
                .toList()
        val markdown =
            root
                .walkTopDown()
                .filter {
                    it.isFile &&
                        it.extension == "md" &&
                        !it.path.contains("build/") &&
                        !it.path.contains(".git/")
                }
                .toList()
        return sources + markdown
    }

    /** The violations of [files]: BOM bytes and U+FFFD, each named with its file and line. */
    private fun violationsIn(files: List<File>): List<String> {
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
        return violations
    }

    @Test
    fun sourcesAndDocsAreCleanUtf8() {
        val root = File("..").canonicalFile
        assertTrue("expected the repository at ${root.absolutePath}", root.isDirectory)

        val files = coveredFiles(root)
        assertTrue("expected sources and docs to scan", files.isNotEmpty())

        val violations = violationsIn(files)
        if (violations.isNotEmpty()) {
            fail("Source encoding violations:\n" + violations.joinToString("\n"))
        }
    }

    @Test
    fun theScanFailsOnAFileWithAReplacementCharacter() {
        val fixtureDir = createTempDirectory(prefix = "encoding")
        try {
            val bad = File(fixtureDir.toFile(), "bad.md")
            bad.writeText("clean line\nbroken ${String(Character.toChars(0xFFFD))} here\n")

            val violations = violationsIn(listOf(bad))

            assertTrue("the scan must report the replacement character", violations.any { it.contains("U+FFFD") })
            assertTrue("the violation must name the file", violations.any { it.contains("bad.md") })
            assertTrue("the violation must name the line", violations.any { it.contains(":2:") })
        } finally {
            fixtureDir.toFile().deleteRecursively()
        }
    }

    private fun hasBom(bytes: ByteArray): Boolean =
        bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
}
