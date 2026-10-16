package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Enforces: at most one top-level type declaration per source file, and when a
 * file declares exactly one, the file is named after it.
 *
 * Scans `app/src/main/java` (Gradle runs unit tests with the module dir as cwd).
 * Top-level declarations start at column 0; nested ones are indented and ignored.
 * Groups of top-level functions are exempt (they declare no types).
 */
class OneTypePerFileTest {

    private val typeDeclaration = Regex(
        """^(?:public |internal |private )?(?:data |sealed |abstract |open |value |enum |annotation )?(?:class |interface |object )([A-Za-z_]\w*)"""
    )

    @Test
    fun oneTypePerFileAndNamedAfterIt() {
        val sources = File("src/main/java")
        assertTrue("expected sources at ${sources.absolutePath}", sources.isDirectory)

        val violations = mutableListOf<String>()
        sources.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val types = file.readLines().mapNotNull { typeDeclaration.find(it)?.groupValues?.get(1) }
                when {
                    types.size > 1 ->
                        violations += "${file.relativeTo(sources).path} declares ${types.size} top-level types: ${types.joinToString()}"

                    types.size == 1 && types.single() != file.nameWithoutExtension ->
                        violations += "${file.relativeTo(sources).path} declares '${types.single()}' but is named '${file.nameWithoutExtension}.kt'"
                }
            }

        if (violations.isNotEmpty()) {
            fail("One type per file violated:\n" + violations.joinToString("\n"))
        }
    }
}
