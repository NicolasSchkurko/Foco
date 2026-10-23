package com.foco.app.data.system

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings

/**
 * OS: the "Usage access" special permission (AppOpsManager PACKAGE_USAGE_STATS); where: the
 * system's app-ops table; lifecycle: checked on demand — never auto-granted.
 */
object UsageAccess {
    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode =
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
