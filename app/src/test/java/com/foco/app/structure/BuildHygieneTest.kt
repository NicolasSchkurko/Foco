package com.foco.app.structure

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The module fails on warnings and CI runs the static analysis: the check reads the configuration
 * that enforces both, not a phrase that could sit anywhere in the build file.
 */
class BuildHygieneTest {
    @Test
    fun warningsAreErrorsForMainAndUnitTestSources() {
        val build = File("build.gradle.kts").readText()
        val compiler =
            blockOf(
                build,
                "tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach",
            )

        assertTrue(
            "the compiler block must turn warnings into errors: $compiler",
            compiler.contains("allWarningsAsErrors.set(true)"),
        )
        assertTrue(
            "the device suite is the only source set excluded from warnings-as-errors: $compiler",
            compiler.contains("AndroidTest"),
        )
    }

    @Test
    fun aLintViolationFailsTheBuild() {
        val build = File("build.gradle.kts").readText()
        val ktlint = blockOf(build, "ktlint {")

        assertTrue(
            "ktlint must not ignore its failures: $ktlint",
            ktlint.contains("ignoreFailures.set(false)"),
        )
    }

    @Test
    fun ciRunsTheSuiteAndStaticAnalysis() {
        val ci = File("../.github/workflows/ci.yml").readText()

        assertTrue("CI must run the JVM suite", ci.contains("./gradlew test"))
        assertTrue("CI must run the static analysis", ci.contains("ktlintCheck"))
    }

    /** The block that opens with [header], braces included; empty when the header is absent. */
    private fun blockOf(
        text: String,
        header: String,
    ): String {
        val start = text.indexOf(header)
        if (start < 0) return ""
        val open = text.indexOf('{', start)
        if (open < 0) return ""
        var depth = 0
        var index = open
        while (index < text.length) {
            when (text[index]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return text.substring(open, index + 1)
                }
            }
            index++
        }
        return ""
    }
}
