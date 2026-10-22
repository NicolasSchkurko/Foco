package com.foco.app.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * OS: the "Notifications" runtime permission (POST_NOTIFICATIONS); where: the system's
 * permission table; lifecycle: checked on demand - never auto-granted.
 */
object NotificationPermission {
    fun isGranted(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun settingsIntent(): Intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
}
