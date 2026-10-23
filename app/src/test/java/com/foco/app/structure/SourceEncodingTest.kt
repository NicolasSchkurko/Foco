package com.foco.app.structure

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Source files and every tracked Markdown are UTF-8 without BOM and carry no replacement
 * characters and no double-encoded text (the bytes of a UTF-8 file read as Latin-1).
 */
class SourceEncodingTest {
    private val replacement = String(Character.toChars(0xFFFD))

    /** The double-encoded signatures: UTF-8 bytes that were read as Latin-1 and written back. */
    private val mojibakeSignatures =
        listOf(
            "\u00E2\u20AC", // the mis-decoded dash, quote, guillemet or ellipsis
            "\u00C3\u00A9", // a mis-decoded e-acute
            "\u00C3\u00A1", // a mis-decoded a-acute
            "\u00C3\u00B3", // a mis-decoded o-acute
            "\u00C3\u00BA", // a mis-decoded u-acute
            "\u00C3\u00B1", // a mis-decoded n-tilde
            "\u00C2\u00A0", // a mis-decoded non-breaking space
        )

    /** The files the rule covers: the sources and every Markdown file of the repository. */
    private fun coveredFiles(root: File): List<File> {
        val ignoredDirs = setOf("build", ".git", ".gradle", ".artifacts", ".idea", "node_modules")
        return root
            .walkTopDown()
            .onEnter { dir -> dir.name !in ignoredDirs }
            .filter { file ->
                file.isFile && file.extension in setOf("kt", "xml", "md")
            }.toList()
    }

    /** The violations of [files]: BOM bytes, U+FFFD and double-encoded text, each named. */
    private fun violationsIn(files: List<File>): List<String> {
        val violations = mutableListOf<String>()
        files.forEach { file ->
            val bytes = file.readBytes()
            if (hasBom(bytes)) violations += "${file.path}: UTF-8 BOM"

            file.readLines().forEachIndexed { index, line ->
                if (line.indexOf(replacement) >= 0) {
                    violations += "${file.path}:${index + 1}: U+FFFD"
                }
                mojibakeSignatures.forEach { signature ->
                    if (line.contains(signature)) {
                        violations += "${file.path}:${index + 1}: double-encoded text"
                    }
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

    /** The fixture proves the scan sees the double-encoded text that no replacement character marks. */
    @Test
    fun theScanFailsOnADoubleEncodedLine() {
        val fixtureDir = createTempDirectory(prefix = "encoding-mojibake")
        try {
            val bad = File(fixtureDir.toFile(), "bad.md")
            bad.writeText("clean line\n10:00${"\u00E2\u20AC\u201C"}10:05\n")

            val violations = violationsIn(listOf(bad))

            assertTrue("the scan must report the double-encoded text: $violations", violations.any { it.contains("double-encoded") })
            assertTrue("the violation must name the file: $violations", violations.any { it.contains("bad.md") })
            assertTrue("the violation must name the line: $violations", violations.any { it.contains(":2:") })
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
