package com.foco.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * La migración del split: `language`/`appearance` viajan del store legado al nuevo,
 * se limpian del legado, las exclusiones no se tocan, y correrla de nuevo es no-op.
 * Dos DataStores reales sobre archivos temporales (sin Android).
 */
class SettingsMigrationTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun newStore(fileName: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temp.root, fileName) }
        )

    /**
     * El artefacto Android de DataStore mueve su archivo temporal con `File.renameTo`
     * cuando `Build.VERSION.SDK_INT < 26` — el valor que ve un test unitario, porque el
     * `android.jar` mockeado reporta 0. En Windows `renameTo` no puede reemplazar un
     * archivo existente, así que la segunda escritura al mismo archivo falla ahí.
     * Ver el change `datastore-test-on-windows`.
     */
    private fun assumeDataStoreCanReplaceAnExistingFile() {
        assumeFalse(
            "En Windows DataStore no puede reemplazar un archivo existente (rama renameTo)",
            System.getProperty("os.name").orEmpty().lowercase().contains("win")
        )
    }

    @After
    fun cleanUp() {
        scope.cancel()
    }

    @Test
    fun copiesTheSettingsClearsTheLegacyAndLeavesTheExclusionsAlone() {
        assumeDataStoreCanReplaceAnExistingFile()
        val legacy = newStore("legacy.preferences_pb")
        val target = newStore("foco_settings.preferences_pb")
        val exclusionsKey = androidx.datastore.preferences.core.stringSetPreferencesKey("excluded_packages")

        runBlocking {
            legacy.edit { preferences ->
                preferences[languageKey] = "es"
                preferences[appearanceKey] = "dark"
                preferences[exclusionsKey] = setOf("com.example.app")
            }

            copySettingsFromLegacyStore(legacy, target)

            assertEquals("es", target.data.first()[languageKey])
            assertEquals("dark", target.data.first()[appearanceKey])
            assertNull(legacy.data.first()[languageKey])
            assertNull(legacy.data.first()[appearanceKey])
            assertEquals(setOf("com.example.app"), legacy.data.first()[exclusionsKey])
        }
    }

    @Test
    fun runningItAgainIsANoOp() {
        assumeDataStoreCanReplaceAnExistingFile()
        val legacy = newStore("legacy.preferences_pb")
        val target = newStore("foco_settings.preferences_pb")

        runBlocking {
            legacy.edit { preferences -> preferences[languageKey] = "en" }
            copySettingsFromLegacyStore(legacy, target)
            copySettingsFromLegacyStore(legacy, target)

            assertEquals("en", target.data.first()[languageKey])
            assertNull(legacy.data.first()[languageKey])
        }
    }

    @Test
    fun aFreshInstallIsUntouched() {
        val legacy = newStore("legacy.preferences_pb")
        val target = newStore("foco_settings.preferences_pb")

        runBlocking {
            copySettingsFromLegacyStore(legacy, target)

            assertEquals(emptyMap<Any, Any>(), target.data.first().asMap())
        }
    }
}
