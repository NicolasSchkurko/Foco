package com.foco.app.data.stores

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsMigrationTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `migrates language and appearance and writes the done marker`() =
        runBlocking {
            val legacy = createDataStore("legacy")
            val target = createDataStore("target")
            legacy.edit {
                it[languageKey] = "es"
                it[appearanceKey] = "dark"
            }

            copySettingsFromLegacyStore(legacy, target)

            val migrated = target.data.first()
            assertEquals("es", migrated[languageKey])
            assertEquals("dark", migrated[appearanceKey])
            assertEquals(true, migrated[migrationDoneKey])

            val remainingLegacy = legacy.data.first()
            assertNull(remainingLegacy[languageKey])
            assertNull(remainingLegacy[appearanceKey])
        }

    @Test
    fun `a second run sees the marker and is a no op`() =
        runBlocking {
            val legacy = createDataStore("legacy")
            val target = createDataStore("target")
            target.edit { it[migrationDoneKey] = true }
            legacy.edit { it[languageKey] = "es" }

            copySettingsFromLegacyStore(legacy, target)

            assertNull(target.data.first()[languageKey])
            assertEquals("es", legacy.data.first()[languageKey])
        }

    @Test
    fun `a failing copy leaves the legacy file untouched`() =
        runBlocking {
            val legacy = createDataStore("legacy")
            legacy.edit { it[languageKey] = "es" }
            val failingTarget = FailingTargetDataStore()

            runCatching { copySettingsFromLegacyStore(legacy, failingTarget) }

            assertEquals("es", legacy.data.first()[languageKey])
        }

    private fun createDataStore(name: String): DataStore<Preferences> {
        val file = File(temp.root, "$name.preferences_pb")
        assumeFalse(
            "DataStore creation fails on Windows when the temporary path carries non-ASCII characters",
            file.absolutePath.any { it.code > 127 },
        )
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    private class FailingTargetDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { emit(PreferenceDataStoreFactory.create(produceFile = { File("") }).data.first()) }

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            error("write failed")
        }
    }
}
