package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** The module fails on warnings and CI runs the static-analysis task. */
class BuildHygieneTest {

    @Test
    fun warningsAreErrorsAndCiRunsStaticAnalysis() {
        val build = File("build.gradle.kts").readText()
        assertTrue("main/unit-test compiler warnings must fail", build.contains("allWarningsAsErrors"))

        val ci = File("../.github/workflows/ci.yml").readText()
        assertTrue("CI must run ktlintCheck", ci.contains("ktlintCheck"))
    }
}
