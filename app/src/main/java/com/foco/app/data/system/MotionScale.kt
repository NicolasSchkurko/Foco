package com.foco.app.data.system

import android.content.Context
import android.provider.Settings

/**
 * OS: the system animator duration scale (Settings.Global); where: device settings;
 * lifecycle: read at every check — 0 means the user asked for no animations.
 */
class MotionScale(
    private val context: Context,
) {
    fun scale(): Float =
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
}
