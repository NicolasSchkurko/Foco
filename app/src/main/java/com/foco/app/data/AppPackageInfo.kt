package com.foco.app.data

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Reads the PackageManager (an app's icon and label). Resource: the system service, no state of its own.
 */
class AppPackageInfo(
    private val context: Context,
) {
    /** Launcher icon (96x96); `null` if the package does not resolve (e.g. uninstalled). */
    fun iconBitmap(packageName: String): Bitmap? =
        runCatching {
            context.packageManager
                .getApplicationIcon(packageName)
                .toBitmap(width = 96, height = 96)
        }.getOrNull()

    /** The app's visible label; falls back to the package name when the system does not resolve it. */
    fun label(packageName: String): String =
        runCatching {
            val manager = context.packageManager
            manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
}
