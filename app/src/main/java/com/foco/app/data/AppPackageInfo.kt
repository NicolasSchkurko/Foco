package com.foco.app.data

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Adaptador de lectura del PackageManager del sistema: ícono de lanzador y label
 * de una app instalada. Recurso físico: el servicio PackageManager de Android;
 * el contexto es de aplicación (vive lo que la app), sin estado propio.
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
