package com.foco.app.ui.common.apps

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.foco.app.data.system.AppPackageInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * OS: the installed apps' icons and labels (PackageManager); where: the process memory, bounded to
 * 64 bitmaps; lifecycle: as long as the process, dropped when the app leaves.
 */
object AppPackageCache {
    private const val ICON_CACHE_SIZE = 64

    private val labels = ConcurrentHashMap<String, String>()
    private val icons = LruCache<String, ImageBitmap>(ICON_CACHE_SIZE)

    @Volatile
    private var adapter: AppPackageInfo? = null

    /** The app's label; read once per package, so a row that scrolls back does not read it again. */
    fun label(
        context: Context,
        packageName: String,
    ): String = labels.getOrPut(packageName) { adapter(context).label(packageName) }

    /** The app's icon; decoded once per package, so a row that scrolls back does not decode it again. */
    fun icon(
        context: Context,
        packageName: String,
    ): ImageBitmap? =
        synchronized(icons) {
            icons.get(packageName)
                ?: adapter(context).iconBitmap(packageName)?.asImageBitmap()?.also { icons.put(packageName, it) }
        }

    private fun adapter(context: Context): AppPackageInfo =
        adapter
            ?: synchronized(this) {
                adapter ?: AppPackageInfo(context.applicationContext).also { adapter = it }
            }
}
