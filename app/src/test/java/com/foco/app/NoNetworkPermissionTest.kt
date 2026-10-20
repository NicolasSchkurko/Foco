package com.foco.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 100 % local: limits and the monitor use private storage and platform APIs, so the manifest
 * must never declare network access. Scans `src/main` (cwd = the module).
 */
class NoNetworkPermissionTest {
    @Test
    fun theManifestDeclaresNoNetworkPermission() {
        val manifest = File("src/main/AndroidManifest.xml")
        assertTrue("expected manifest at ${manifest.absolutePath}", manifest.isFile)

        val text = manifest.readText()

        assertFalse(
            "the manifest must not request android.permission.INTERNET",
            text.contains("android.permission.INTERNET"),
        )
        assertFalse(
            "the manifest must not request android.permission.ACCESS_NETWORK_STATE",
            text.contains("android.permission.ACCESS_NETWORK_STATE"),
        )
    }
}
