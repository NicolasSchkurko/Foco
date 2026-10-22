package com.foco.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The app's label, resolved off the main thread (labels belong to the UI). */
@Composable
fun appLabel(packageName: String): String {
    val context = LocalContext.current
    val label by produceState(initialValue = packageName, packageName) {
        value =
            withContext(Dispatchers.IO) {
                AppPackageCache.label(context, packageName)
            }
    }
    return label
}
