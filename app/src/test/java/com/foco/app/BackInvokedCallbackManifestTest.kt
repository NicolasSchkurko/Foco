package com.foco.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test for the Android 16 predictive-back bug: since API 36 the system gesture
 * is delivered to an OnBackInvokedCallback, which the activity only registers when the
 * manifest declares `android:enableOnBackInvokedCallback="true"`. Without the flag the
 * gesture silently stopped working (the back key and the visible control kept working).
 *
 * Scans `src/main/AndroidManifest.xml` (Gradle runs unit tests with the module dir as cwd).
 */
class BackInvokedCallbackManifestTest {

    @Test
    fun manifestDeclaresEnableOnBackInvokedCallback() {
        val manifest = File("src/main/AndroidManifest.xml")
        assertTrue("expected manifest at ${manifest.absolutePath}", manifest.isFile)
        assertTrue(
            "AndroidManifest.xml must declare android:enableOnBackInvokedCallback=\"true\" on <application>",
            manifest.readText().contains("android:enableOnBackInvokedCallback=\"true\"")
        )
    }
}
