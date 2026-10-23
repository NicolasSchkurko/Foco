package com.foco.app.data.system

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * The accessibility overlay window: adds and removes a view on the system window manager.
 * Resource: the WINDOW_SERVICE of the given context; one instance per block.
 */
class OverlayWindow(
    context: Context,
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)

    fun add(view: View): Boolean =
        runCatching { windowManager.addView(view, params()) }
            .onFailure { Log.w(TAG, "The overlay window was rejected", it) }
            .isSuccess

    fun remove(view: View) {
        runCatching { windowManager.removeView(view) }
    }

    private fun params(): WindowManager.LayoutParams =
        WindowManager
            .LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT,
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }

    private companion object {
        const val TAG = "FocoOverlay"
    }
}
