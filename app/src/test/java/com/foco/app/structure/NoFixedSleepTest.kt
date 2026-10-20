package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Instrumented tests wait for the state they need; a fixed `Thread.sleep` hides the reason and
 * costs the whole duration even when the state is already there.
 */
class NoFixedSleepTest {

    @Test
    fun noFixedSleepInInstrumentedTests() {
        val sources = File("src/androidTest")
        assertTrue("expected sources at ${sources.absolutePath}", sources.isDirectory)

        val offenders = sources.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> line.contains("Thread.sleep") }
                    .map { (index, _) -> "${file.relativeTo(sources).path}:${index + 1}" }
            }
            .toList()

        if (offenders.isNotEmpty()) {
            fail(
                "fixed Thread.sleep in androidTest; wait for the expected state instead:\n" +
                    offenders.joinToString("\n")
            )
        }
    }
}
