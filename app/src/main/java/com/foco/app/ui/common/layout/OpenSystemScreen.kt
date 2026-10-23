package com.foco.app.ui.common.layout

import android.content.Context
import android.content.Intent

/**
 * OS: the system screens the app opens (a permission's settings, the repository); where: the
 * platform's activity manager; lifecycle: the intent is launched and the screen takes over.
 */
fun openSystemScreen(
    context: Context,
    intent: Intent,
) {
    context.startActivity(intent)
}
