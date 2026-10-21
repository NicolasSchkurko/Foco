package com.foco.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
import java.io.File

/**
 * The split's migration: `language`/`appearance` travel to the new store, the legacy one is cleared and
 * running it again is a no-op; two real DataStores over temporary files (no Android).
 */
class SettingsMigrationTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun newStore(fileName: String): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temp.root, fileName) },
        )

    /**
     * The Android artifact of DataStore uses `File.renameTo` with `SDK_INT < 26` (what a unit test sees)
     * and on Windows that cannot replace an existing file: see the `datastore-test-on-windows` change.
     */
    private fun assumeDataStoreCanReplaceAnExistingFile() {
        assumeFalse(
            "On Windows DataStore cannot replace an existing file (renameTo branch)",
            System
                .getProperty("os.name")
                .orEmpty()
                .lowercase()
                .contains("win"),
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
        val exclusionsKey =
            androidx.datastore.preferences.core
                .stringSetPreferencesKey("excluded_packages")

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
    fun targetExistingValueNotOverwrittenByLegacy() {
        assumeDataStoreCanReplaceAnExistingFile()
        val legacy = newStore("legacy_existing.preferences_pb")
        val target = newStore("foco_existing.preferences_pb")

        runBlocking {
            target.edit { preferences -> preferences[languageKey] = "fr" }
            legacy.edit { preferences -> preferences[languageKey] = "de" }

            copySettingsFromLegacyStore(legacy, target)

            assertEquals("fr", target.data.first()[languageKey]) // preserved, not overwritten with "de"
            assertNull(legacy.data.first()[languageKey])
        }
    }

    @Test
    fun partialCopyOnlyMissingKeyWhenTargetHasOthers() {
        assumeDataStoreCanReplaceAnExistingFile()
        val legacy = newStore("legacy_partial.preferences_pb")
        val target = newStore("foco_partial.preferences_pb")

        runBlocking {
            target.edit { preferences -> preferences[languageKey] = "fr" }
            legacy.edit { preferences ->
                preferences[languageKey] = "de"
                preferences[appearanceKey] = "dark"
            }

            copySettingsFromLegacyStore(legacy, target)

            assertEquals("fr", target.data.first()[languageKey])
            assertEquals("dark", target.data.first()[appearanceKey])
            assertNull(legacy.data.first()[languageKey])
            assertNull(legacy.data.first()[appearanceKey])
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
