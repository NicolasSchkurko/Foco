package com.foco.app.data

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * Chequeo del permiso especial "Acceso de uso" (PACKAGE_USAGE_STATS).
 *
 * No es un permiso runtime: no existe diálogo que lo otorgue.
 * El usuario lo activa en Ajustes > Acceso especial > Acceso de uso.
 * A nivel sistema se verifica como un AppOp.
 */
object UsageAccess {

    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
