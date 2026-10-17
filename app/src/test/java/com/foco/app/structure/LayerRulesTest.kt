package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Enforces the layer rules:
 * 1. Every source lives in the folder that matches its package.
 * 2. `model/` and `domain/` do not import Android or Compose APIs.
 * 3. Dependencies only point downward: ui → data → domain → model.
 * 4. `data/` is Android platform access: every file imports android/androidx.
 * 5. `ui/` never reaches the platform raw — reads go through `data/` adapters.
 * 6. UI features do not import each other; shared widgets live in `ui/common/`.
 *
 * Scans `app/src/main/java` (Gradle runs unit tests with the module dir as cwd).
 * Root-package files (`com.foco.app`, e.g. the entry activity) are exempt from the
 * dependency direction rule.
 */
class LayerRulesTest {

    private val packageLine = Regex("""^package\s+([\w.]+)""")
    private val appImport = Regex("""^import\s+com\.foco\.app\.(\w+)""")
    private val frameworkImport = Regex("""^import\s+(android|androidx)\.""")
    private val forbiddenLayers = mapOf(
        "model" to setOf("domain", "data", "ui"),
        "domain" to setOf("data", "ui"),
        "data" to setOf("ui"),
        "ui" to emptySet<String>()
    )
    /** Raw platform calls forbidden in `ui/` (heuristic marker scan). */
    private val rawPlatformMarkers = listOf(
        "packageManager.",
        "getSystemService(",
        "queryEvents(",
        "unsafeCheckOpNoThrow("
    )
    /** Feature packages under `ui/`; imports between two of them are violations. */
    private val uiFeatures = setOf("dashboard", "detail", "settings", "onboarding")
    private val uiFeatureImport = Regex("""^import\s+com\.foco\.app\.ui\.(\w+)""")

    @Test
    fun packagesMatchFoldersPureLayersStayPureAndDependenciesPointDownward() {
        val sources = File("src/main/java")
        assertTrue("expected sources at ${sources.absolutePath}", sources.isDirectory)

        val violations = mutableListOf<String>()
        sources.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relative = file.relativeTo(sources).path.replace('\\', '/')
                val lines = file.readLines()
                val declaredPackage = lines.firstNotNullOfOrNull { packageLine.find(it)?.groupValues?.get(1) }

                if (declaredPackage == null) {
                    violations += "$relative has no package declaration"
                    return@forEach
                }

                val expectedFolder = declaredPackage.replace('.', '/')
                val actualFolder = relative.substringBeforeLast('/')
                if (actualFolder != expectedFolder) {
                    violations += "$relative: package '$declaredPackage' expects folder '$expectedFolder'"
                }

                // com.foco.app.<layer>[.sub..] → layer; root files (com.foco.app) are exempt.
                val layer = declaredPackage.split('.').getOrNull(3) ?: return@forEach
                val imports = lines.filter { it.startsWith("import ") }

                if (layer == "model" || layer == "domain") {
                    imports.filter { frameworkImport.containsMatchIn(it) }
                        .forEach { violations += "$relative: pure layer '$layer' imports a framework API → $it" }
                }

                if (layer == "data" && imports.none { frameworkImport.containsMatchIn(it) }) {
                    violations += "$relative: 'data' layer is Android platform access — a file without android/androidx imports belongs in 'domain/'"
                }

                if (layer == "ui") {
                    rawPlatformMarkers.forEach { marker ->
                        if (lines.any { marker in it }) {
                            violations += "$relative: 'ui' layer reaches the platform raw ('$marker') — go through a 'data/' adapter"
                        }
                    }

                    val feature = declaredPackage.split('.').getOrNull(4)
                    if (feature != null && feature in uiFeatures) {
                        imports.forEach { line ->
                            val target = uiFeatureImport.find(line)?.groupValues?.get(1)
                            if (target != null && target in uiFeatures && target != feature) {
                                violations += "$relative: ui feature '$feature' must not import '$target' — share through 'ui/common/'"
                            }
                        }
                    }
                }

                val forbidden = forbiddenLayers[layer] ?: return@forEach
                imports.forEach { line ->
                    val target = appImport.find(line)?.groupValues?.get(1)
                    if (target != null && target in forbidden) {
                        violations += "$relative: '$layer' layer must not depend on '$target' → $line"
                    }
                }
            }

        if (violations.isNotEmpty()) {
            fail("Layer rules violated:\n" + violations.joinToString("\n"))
        }
    }
}
