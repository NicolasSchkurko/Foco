package com.foco.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regresión del back predictivo en Android 16: sin `enableOnBackInvokedCallback="true"` el gesto se
 * pierde. Escanea `src/main/AndroidManifest.xml` (cwd = módulo).
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
