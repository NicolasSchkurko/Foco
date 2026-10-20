package com.foco.app.data.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.domain.schemaDrift
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Category: schema baseline (API 36). Opens the real database and compares its identity hash with the
 * committed export, so an entity edit without a version bump fails here instead of shipping.
 */
@RunWith(AndroidJUnit4::class)
class SchemaBaselineTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val databaseName = "schema-baseline-test.db"

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun theCommittedSchemaMatchesTheOpenDatabase() {
        val database =
            Room
                .databaseBuilder(context, FocoDatabase::class.java, databaseName)
                .addMigrations(FocoDatabase.MIGRATION_1_2)
                .build()
        val version =
            database.openHelper.readableDatabase
                .query("PRAGMA user_version")
                .use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }
        val runtimeHash =
            database.openHelper.readableDatabase
                .query("SELECT identity_hash FROM room_master_table")
                .use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        database.close()

        val committed = committedSchemas()
        assertTrue("no committed schema for version $version", version in committed)

        val drift = schemaDrift(version, committed.keys, committed[version], runtimeHash)

        assertNull("schema drift: $drift", drift)
    }

    @Test
    fun newestCommittedExportCarriesAnIdentityHash() {
        val newest = committedSchemas().keys.max()

        assertNotNull(committedSchemas()[newest])
    }

    /** The committed exports, read from the schemas folder wired as androidTest assets. */
    private fun committedSchemas(): Map<Int, String?> {
        val assets = instrumentation.context.assets
        val folder = "com.foco.app.data.db.FocoDatabase"
        return (assets.list(folder) ?: emptyArray())
            .mapNotNull { name -> name.removeSuffix(".json").toIntOrNull()?.let { it to name } }
            .associate { (version, name) ->
                val json = assets.open("$folder/$name").bufferedReader().use { it.readText() }
                version to identityHashOf(json)
            }
    }

    private fun identityHashOf(json: String): String? = Regex("\"identityHash\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1)
}
