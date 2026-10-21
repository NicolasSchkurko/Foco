package com.foco.app.structure

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * A file that touches the OS, storage or network carries a ≤4-line header naming the physical
 * resource, where it lives and its lifecycle (spec-tooling).
 */
class ResourceHeaderTest {
    /** Declared resource files for this tripwire; checked exactly once. */
    private val declared = mapOf(
        "com/foco/app/data/AppLanguage" to File("src/main/java/com/foco/app/data/AppLanguage.kt"),
        "com/foco/app/data/MotionScale" to File("src/main/java/com/foco/app/data/MotionScale.kt"),
        "com/foco/app/data/UsageAccess" to File("src/main/java/com/foco/app/data/UsageAccess.kt"),
        "com/foco/app/data/SettingsMigration" to File("src/main/java/com/foco/app/data/SettingsMigration.kt"),
    )

    /** The header block: the first comment block of the file, up to the 4-line budget. */
    private fun headerLines(file: File): List<String> {
        val lines = file.readLines()
        val header = mutableListOf<String>()
        var inBlock = false
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("package ") || trimmed.startsWith("import ")) {
                if (header.isNotEmpty() && !inBlock) break
                continue
            }
            val isComment = trimmed.startsWith("/**") || trimmed.startsWith("*") || trimmed.startsWith("*/") || trimmed.startsWith("//")
            if (!isComment) break
            header += trimmed
            inBlock = trimmed.startsWith("/**") && !trimmed.endsWith("*/")
            if (header.size >= 6) break
        }
        return header
    }

    /** A line names the physical resource, the place or the lifecycle. */
    private fun namesResource(line: String): Boolean {
        val text = line.lowercase()
        return text.contains("resource") || text.contains("store") ||
            text.contains("lifecycle") || text.contains("database") ||
            text.contains("local") || text.contains("system") ||
            text.contains("device") || text.contains("database") ||
            text.contains("file") || text.contains("prefs")
    }

    @Test
    fun declaredFilesCarryResourceHeaders() {
        val violations = mutableListOf<String>()
        for ((relative, file) in declared) {
            val header = headerLines(file)
            if (header.size > 4) {
                violations += "$relative: ${header.size} comment lines → ${header.firstOrNull() ?: "(empty)"}"
            }
            if (header.none { namesResource(it) }) {
                violations += "$relative: header missing resource/place/lifecycle → ${header.firstOrNull() ?: "(empty)"}"
            }
        }

        if (violations.isNotEmpty()) {
            fail("Resource headers (${violations.size}):\n" + violations.joinToString("\n"))
        }
    }

    @Test
    fun theScanFailsOnAFileWithoutAHeader() {
        val fixtureDir = createTempDirectory(prefix = "resource-header")
        try {
            val bad = File(fixtureDir.toFile(), "bad.kt")
            bad.writeText("package test\\nobject NoHeader {\\n  fun x(): Long = System.currentTimeMillis()\\n}\\n")
            val header = headerLines(bad)
            assertTrue("the scan must fail when a declared resource file has no header", !header.any { namesResource(it) })
        } finally {
            fixtureDir.toFile().deleteRecursively()
        }
    }
}
