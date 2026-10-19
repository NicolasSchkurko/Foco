package com.foco.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.foco.app.data.AppPackageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The app's label, resolved off the main thread (labels belong to the UI). */
@Composable
fun appLabel(packageName: String): String {
    val context = LocalContext.current
    val packageInfo = remember { AppPackageInfo(context.applicationContext) }
    val label by produceState(initialValue = packageName, packageName) {
        value = withContext(Dispatchers.IO) {
            packageInfo.label(packageName)
        }
    }
    return label
}
