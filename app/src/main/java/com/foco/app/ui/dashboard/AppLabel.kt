package com.foco.app.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.foco.app.data.AppPackageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Label de la app, resuelto fuera del hilo principal (los labels viven en la UI). */
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
