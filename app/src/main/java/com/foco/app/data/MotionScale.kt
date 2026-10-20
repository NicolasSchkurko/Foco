package com.foco.app.data

import android.content.Context
import android.provider.Settings

/** The system animator duration scale; 0 means the user asked for no animations. */
class MotionScale(private val context: Context) {
    fun scale(): Float = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
}
