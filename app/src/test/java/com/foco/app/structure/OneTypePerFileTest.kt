package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * One top-level type per file, named after the file; groups of functions
 * are exempt. Scans `src/main/java` (declarations at column 0; nested ones do not count).
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
