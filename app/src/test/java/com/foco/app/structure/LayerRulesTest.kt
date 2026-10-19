package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Aplica las reglas de capas de `openspec/specs/code-structure` (carpeta ↔ paquete, pureza,
 * dirección hacia abajo, features que no se importan). Escanea `src/main/java` (cwd = módulo).
 */
class LayerRulesTest {

    @get:Rule
    val temp = TemporaryFolder()

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
    /**
     * Carpetas compartidas bajo `ui/`: no son features. `common`, `metrics` y `theme` son las que
     * la spec exime, y `ui/common/` es la casa del reuso entre pantallas.
     */
    private val sharedUiFolders = setOf("common", "metrics", "theme")
    private val uiFeatureImport = Regex("""^import\s+com\.foco\.app\.ui\.(\w+)""")

    /** Una carpeta bajo `ui/` es feature si no es compartida: no hay lista que ampliar a mano. */
    private fun isUiFeature(folder: String): Boolean = folder !in sharedUiFolders

    /** Las features reales, derivadas del árbol: subcarpetas de `ui/` que no son compartidas. */
    private fun uiFeaturesIn(sources: File): Set<String> =
        File(sources, "com/foco/app/ui")
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .map { it.name }
            .filter(::isUiFeature)
            .toSet()

    @Test
    fun packagesMatchFoldersPureLayersStayPureAndDependenciesPointDownward() {
        val violations = violationsIn(File("src/main/java"))

        if (violations.isNotEmpty()) {
            fail("Layer rules violated:\n" + violations.joinToString("\n"))
        }
    }

    /**
     * Escanea [sources] con las reglas de capas y devuelve las violaciones (vacío = el árbol las respeta);
     * la raíz entra por parámetro para poder correr lo mismo sobre un árbol sintético.
     */
    private fun violationsIn(sources: File): List<String> {
        assertTrue("expected sources at ${sources.absolutePath}", sources.isDirectory)

        // Las features salen del árbol: cualquier carpeta bajo `ui/` que no sea compartida.
        val uiFeatures = uiFeaturesIn(sources)

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

        return violations
    }

    @Test
    fun theUiFeatureSetIsDerivedFromTheTreeNotFromAHandWrittenList() {
        val sources = File("src/main/java")
        val derived = uiFeaturesIn(sources)

        // Una carpeta nueva bajo `ui/` ya cuenta como feature, sin tocar el chequeo. La lista fija
        // anterior (`dashboard`, `detail`, `settings`, `onboarding`) dejaba "reports" fuera de la regla.
        assertTrue("a folder that is not shared must be a feature", isUiFeature("reports"))

        sharedUiFolders.forEach { assertFalse("shared '$it' must not be a feature", isUiFeature(it)) }

        assertTrue(
            "expected the current UI features, got $derived",
            derived.containsAll(setOf("dashboard", "detail", "settings", "onboarding"))
        )
        assertTrue(
            "shared folders must not be read as features, got $derived",
            derived.none { it in sharedUiFolders }
        )
    }

    @Test
    fun theScanReportsAViolationFromAFeatureFolderItHasNeverSeen() {
        val root = File(temp.root, "src/main/java")
        writeKotlinFile(root, "com/foco/app/ui/dashboard/Row.kt", "com.foco.app.ui.dashboard")
        writeKotlinFile(
            root, "com/foco/app/ui/common/Widget.kt", "com.foco.app.ui.common",
            "com.foco.app.ui.dashboard.Row"
        )
        writeKotlinFile(
            root, "com/foco/app/ui/reports/Reports.kt", "com.foco.app.ui.reports",
            "com.foco.app.ui.dashboard.Row"
        )

        val violations = violationsIn(root)

        assertEquals("the scan must report the new feature once: $violations", 1, violations.size)
        assertTrue(
            "the violation must name the file: $violations",
            violations.single().contains("ui/reports/Reports.kt")
        )
        assertTrue(
            "a shared folder must not be reported as a feature: $violations",
            violations.none { it.startsWith("com/foco/app/ui/common/") }
        )
    }

    /** Escribe un fuente sintético: su `package` y los [imports] que se le pasen. */
    private fun writeKotlinFile(root: File, path: String, packageName: String, vararg imports: String) {
        val file = File(root, path)
        file.parentFile.mkdirs()
        file.writeText(
            (listOf("package $packageName") + imports.map { "import $it" })
                .joinToString(separator = "\n", postfix = "\n")
        )
    }
}
