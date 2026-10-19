package com.foco.app.data

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Lee el PackageManager (ícono y label de una app). Recurso: el servicio del sistema, sin estado propio.
 */
class AppPackageInfo(private val context: Context) {

    /** Ícono de lanzador (96×96); `null` si el paquete no resuelve (p. ej. desinstalado). */
    fun iconBitmap(packageName: String): Bitmap? =
        runCatching {
            context.packageManager
                .getApplicationIcon(packageName)
                .toBitmap(width = 96, height = 96)
        }.getOrNull()

    /** Label visible de la app; cae al propio packageName si el sistema no lo resuelve. */
    fun label(packageName: String): String =
        runCatching {
            val manager = context.packageManager
            manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
}
