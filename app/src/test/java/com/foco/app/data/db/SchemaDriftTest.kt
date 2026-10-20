package com.foco.app.data.db

import com.foco.app.domain.schemaDrift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SchemaDriftTest {
    @Test
    fun `an edit without a bump drifts`() {
        val drift =
            schemaDrift(
                declaredVersion = 2,
                committedVersions = setOf(1, 2),
                committedIdentityHash = "aaaa",
                runtimeIdentityHash = "bbbb",
            )

        assertTrue(drift!!.contains("drifted"))
        assertTrue(drift.contains("2"))
        assertTrue(drift.contains("aaaa"))
        assertTrue(drift.contains("bbbb"))
    }

    @Test
    fun `a bump without an export reports the missing schema`() {
        val drift =
            schemaDrift(
                declaredVersion = 3,
                committedVersions = setOf(1, 2),
                committedIdentityHash = null,
                runtimeIdentityHash = "bbbb",
            )

        assertEquals("version 3 has no committed schema export", drift)
    }

    @Test
    fun `the consistent baseline reports no drift`() {
        val drift =
            schemaDrift(
                declaredVersion = 2,
                committedVersions = setOf(1, 2),
                committedIdentityHash = "same",
                runtimeIdentityHash = "same",
            )

        assertNull(drift)
    }

    @Test
    fun `a committed schema without a hash is drift`() {
        val drift =
            schemaDrift(
                declaredVersion = 2,
                committedVersions = setOf(2),
                committedIdentityHash = "",
                runtimeIdentityHash = "bbbb",
            )

        assertEquals("version 2 has no identity hash in its committed schema", drift)
    }
}
