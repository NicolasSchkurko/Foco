package com.foco.app.ui.limits

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.foco.app.data.OverlayWindow

/**
 * Hosts one composable in the accessibility overlay window. Resource: a fresh owner per block and
 * the data-layer [OverlayWindow]; dismissed after each block, never reused.
 */
class BlockOverlayHost(
    private val context: Context,
) {
    private val window = OverlayWindow(context)
    private var owner: OverlayLifecycleOwner? = null
    private var view: ComposeView? = null

    val isShowing: Boolean get() = view != null

    /** Adds the overlay. Returns false when one is already showing or the window refused it. */
    fun show(content: @Composable () -> Unit): Boolean {
        if (view != null) return false
        val blockOwner = OverlayLifecycleOwner()
        blockOwner.prepare()
        val composeView =
            ComposeView(context).apply {
                setViewTreeLifecycleOwner(blockOwner)
                setViewTreeSavedStateRegistryOwner(blockOwner)
                setContent(content)
            }
        if (!window.add(composeView)) {
            blockOwner.destroy()
            return false
        }
        owner = blockOwner
        view = composeView
        blockOwner.resume()
        return true
    }

    fun dismiss() {
        val current = view ?: return
        view = null
        window.remove(current)
        runCatching { current.disposeComposition() }
        owner?.destroy()
        owner = null
    }
}
