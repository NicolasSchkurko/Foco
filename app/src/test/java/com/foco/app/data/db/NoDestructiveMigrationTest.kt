package com.foco.app.data.db

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NoDestructiveMigrationTest {

    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun `production sources never configure a destructive fallback`() {
        val sources = File("src/main")
        assertTrue("expected sources at ${sources.absolutePath}", sources.isDirectory)

        val offenders = offendersIn(sources)
        if (offenders.isNotEmpty()) {
            fail("fallbackToDestructiveMigration in: " + offenders.joinToString(", "))
        }
    }

    @Test
    fun `the scan bites on a source that configures it`() {
        val root = temporary.newFolder("fixture")
        File(root, "Offender.kt").writeText(
            "Room.databaseBuilder(context, Db::class.java, \"x\")\n" +
                "    .fallbackToDestructiveMigration()\n" +
                "    .build()"
        )

        val offenders = offendersIn(root)

        assertTrue(offenders.any { it.endsWith("Offender.kt") })
    }

    private fun offendersIn(root: File): List<String> =
        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.readText().contains("fallbackToDestructiveMigration") }
            .map { it.relativeTo(root).path }
            .toList()
}
