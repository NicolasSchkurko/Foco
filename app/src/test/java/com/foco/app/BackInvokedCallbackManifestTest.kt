package com.foco.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Predictive-back regression on Android 16: without `enableOnBackInvokedCallback="true"` the gesture is
 * lost. It scans `src/main/AndroidManifest.xml` (cwd = the module).
 */
class BackInvokedCallbackManifestTest {
    @Test
    fun manifestDeclaresEnableOnBackInvokedCallback() {
        val manifest = File("src/main/AndroidManifest.xml")
        assertTrue("expected manifest at ${manifest.absolutePath}", manifest.isFile)
        assertTrue(
            "AndroidManifest.xml must declare android:enableOnBackInvokedCallback=\"true\" on <application>",
            manifest.readText().contains("android:enableOnBackInvokedCallback=\"true\""),
        )
    }
}
