package com.foco.app.structure

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * A file that touches the OS, the database, storage or the network carries a ≤4-line header naming
 * the physical resource, where it lives and its lifecycle. The scope comes from the tree.
 */
class ResourceHeaderTest {
    /** The imports that mean the file reaches the OS, the database, storage or the network. */
    private val resourceImports =
        listOf(
            "androidx.room.",
            "androidx.datastore.",
            "androidx.work.",
            "android.app.usage.",
            "android.app.NotificationManager",
            "android.view.WindowManager",
            "android.provider.Settings",
            "android.content.pm.PackageManager",
            "androidx.appcompat.app.AppCompatDelegate",
            "android.accessibilityservice.",
        )

    /** The words that name a physical resource; the header must mention one of them. */
    private val resourceWords =
        listOf(
            "database",
            "table",
            "\\broom\\b",
            "datastore",
            "\\bstore\\b",
            "prefs",
            "preferences",
            "notification",
            "shade",
            "window manager",
            "window service",
            "overlay",
            "settings",
            "app-ops",
            "packagemanager",
            "package manager",
            "usagestats",
            "usage stats",
            "workmanager",
            "work manager",
            "\\bjob\\b",
            "\\bworker\\b",
            "appcompat",
            "night mode",
            "\\blocale\\b",
            "broadcast",
            "binding",
        )

    /** The header names the resource: a label, a physical name in backticks or a resource word. */
    private val resourceLabel = Regex("""(?i)\b(resource|os|where|lifecycle)\s*:""")
    private val physicalName = Regex("""`[^`]+`""")

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

    /** Whether [header] names the physical resource the file touches. */
    private fun namesResource(header: List<String>): Boolean {
        val text = header.joinToString(" ")
        if (resourceLabel.containsMatchIn(text)) return true
        if (physicalName.containsMatchIn(text)) return true
        return resourceWords.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(text) }
    }

    /** The folder of [file] under [appRoot]: the layer it lives in ("" for the entry layer). */
    private fun layerOf(
        appRoot: File,
        file: File,
    ): String {
        val parent = file.parentFile ?: appRoot
        return parent.relativeTo(appRoot).path.replace('\\', '/')
    }

    /** Whether [file] reaches the OS, the database, storage or the network. */
    private fun touchesAResource(file: File): Boolean =
        file.readLines().any { line ->
            line.startsWith("import ") && resourceImports.any { marker -> marker in line }
        }

    /**
     * The files under [appRoot] that must carry a header: the adapters of the data layer and of
     * the entry layer that reach a resource. The schema package and the UI are not adapters.
     */
    private fun filesInScope(appRoot: File): List<File> {
        val kotlinFiles = appRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }
        val adapters =
            kotlinFiles.filter { file ->
                val layer = layerOf(appRoot, file)
                layer.isEmpty() || layer == "data"
            }
        return adapters.filter { file -> touchesAResource(file) }.toList()
    }

    /** The header violations under [appRoot]; each names the file and what the header lacks. */
    private fun violationsIn(appRoot: File): List<String> {
        val violations = mutableListOf<String>()
        for (file in filesInScope(appRoot)) {
            val relative = file.relativeTo(appRoot).path.replace('\\', '/')
            val header = headerLines(file)
            if (header.size > 4) {
                violations += "$relative: ${header.size} comment lines → ${header.firstOrNull() ?: "(empty)"}"
            }
            if (!namesResource(header)) {
                violations += "$relative: header missing resource/place/lifecycle → ${header.firstOrNull() ?: "(empty)"}"
            }
        }
        return violations
    }

    @Test
    fun theFilesThatTouchAResourceCarryAResourceHeader() {
        val appRoot = File("src/main/java/com/foco/app")
        assertTrue("expected sources at ${appRoot.absolutePath}", appRoot.isDirectory)

        val violations = violationsIn(appRoot)
        if (violations.isNotEmpty()) {
            fail("Resource headers (${violations.size}):\n" + violations.joinToString("\n"))
        }
    }

    /** The scope is derived from the tree: a new adapter joins it with no edit to the check. */
    @Test
    fun theScopeComesFromTheTreeAndNotFromAHandWrittenList() {
        val appRoot = File("src/main/java/com/foco/app")
        val scope = filesInScope(appRoot).map { it.relativeTo(appRoot).path.replace('\\', '/') }

        assertTrue("the adapters of the data layer must be in scope: $scope", scope.contains("data/AppLimitsStore.kt"))
        assertTrue("the entry layer's adapters must be in scope: $scope", scope.contains("AccessibilityAccess.kt"))
        assertTrue("the schema package declares tables, it does not open a resource: $scope", scope.none { it.startsWith("data/db/") })
        assertTrue("the UI is a view, not an adapter: $scope", scope.none { it.startsWith("ui/") })
    }

    /** The fixture proves the scan reports a file whose header does not name the resource. */
    @Test
    fun theScanReportsAFileWithoutAResourceHeader() {
        val fixtureDir = createTempDirectory(prefix = "resource-header")
        try {
            val appRoot = File(fixtureDir.toFile(), "com/foco/app")
            val bad = File(appRoot, "data/LegacyStore.kt")
            File(appRoot, "data").mkdirs()
            bad.writeText(
                """
                package com.foco.app.data

                import androidx.room.Room

                class LegacyStore {
                    fun x(): Long = System.currentTimeMillis()
                }
                """.trimIndent(),
            )

            val violations = violationsIn(appRoot)

            assertTrue("the scan must report the missing header: $violations", violations.any { it.contains("LegacyStore.kt") })
            assertTrue("the violation must name the missing resource: $violations", violations.any { it.contains("missing resource") })
        } finally {
            fixtureDir.toFile().deleteRecursively()
        }
    }

    /** The fixture proves the scan accepts a header that names the resource, the place and the life. */
    @Test
    fun theScanAcceptsAHeaderThatNamesTheResource() {
        val fixtureDir = createTempDirectory(prefix = "resource-header-ok")
        try {
            val appRoot = File(fixtureDir.toFile(), "com/foco/app")
            val good = File(appRoot, "data/GoodStore.kt")
            File(appRoot, "data").mkdirs()
            good.writeText(
                """
                package com.foco.app.data

                import androidx.room.Room

                /**
                 * The store. Resource: the `legacy.db` table in the private `limits.db`; lives as
                 * long as the app.
                 */
                class GoodStore
                """.trimIndent(),
            )

            val violations = violationsIn(appRoot)

            assertTrue("a header that names the resource must pass: $violations", violations.none { it.contains("GoodStore.kt") })
        } finally {
            fixtureDir.toFile().deleteRecursively()
        }
    }
}
