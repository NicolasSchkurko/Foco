package com.foco.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * No usage data may leave the device: Auto Backup would copy the database to the cloud.
 * It scans `src/main/AndroidManifest.xml` (cwd = the module).
 */
class BackupDisabledManifestTest {

    @Test
    fun manifestDisablesBackup() {
        val manifest = File("src/main/AndroidManifest.xml")
        assertTrue("expected manifest at ${manifest.absolutePath}", manifest.isFile)
        val text = manifest.readText()

        assertTrue(
            "AndroidManifest.xml must declare android:allowBackup=\"false\" on <application>",
            text.contains("android:allowBackup=\"false\"")
        )
        assertFalse("android:allowBackup=\"true\" must not appear", text.contains("android:allowBackup=\"true\""))
    }
}
