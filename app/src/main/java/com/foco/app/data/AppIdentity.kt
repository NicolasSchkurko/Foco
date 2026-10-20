package com.foco.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * The app's identity for the About block: the repository address and the installed version.
 * Resource: the PackageManager, read-only, no state of its own.
 */
class AppIdentity(
    private val context: Context,
) {
    /** The installed version name; null when the package manager does not resolve it. */
    fun versionName(): String? =
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()

    /** A VIEW intent on the repository; the caller starts it. */
    fun repositoryIntent(): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL))

    companion object {
        const val REPOSITORY_URL = "https://github.com/NicolasSchkurko/Foco"
    }
}
