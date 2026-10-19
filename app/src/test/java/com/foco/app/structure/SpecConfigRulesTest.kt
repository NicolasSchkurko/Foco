package com.foco.app.structure

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * La config del repo no puede descartar sus propias reglas: un ítem de `rules:` sin comillas y con
 * `: ` adentro se lee como mapping y OpenSpec ignora el artefacto entero. Chequeo de forma, línea a línea.
 */
class SpecConfigRulesTest {

    @Test
    fun everyRulesItemIsAQuotedScalar() {
        val config = File("../openspec/config.yaml")
        assertTrue("expected the repo config at ${config.absolutePath}", config.isFile)

        val items = mutableListOf<Pair<Int, String>>()
        var inRules = false
        config.readLines().forEachIndexed { index, line ->
            when {
                line.matches(Regex("""^rules:\s*$""")) -> inRules = true
                inRules && line.isNotBlank() && !line.startsWith(" ") && !line.startsWith("#") -> inRules = false
            }
            if (!inRules) return@forEachIndexed
            val item = Regex("""^\s+-\s+(.*)$""").find(line)?.groupValues?.get(1) ?: return@forEachIndexed
            items += (index + 1) to item
        }

        assertTrue("expected rules items in the config", items.isNotEmpty())
        val violations = items.filter { (_, item) ->
            !(item.startsWith("\"") || item.startsWith("'"))
        }
        if (violations.isNotEmpty()) {
            fail(
                "Rules the tooling would silently drop:\n" +
                    violations.joinToString("\n") { (line, item) ->
                        "  openspec/config.yaml:$line: unquoted item (a ': ' inside it reads as a mapping) → $item"
                    }
            )
        }
    }
}
