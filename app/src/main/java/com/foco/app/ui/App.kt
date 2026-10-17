package com.foco.app.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.foco.app.data.UsageAccess
import com.foco.app.ui.onboarding.OnboardingScreen

/**
 * Decide onboarding ↔ dashboard según el permiso de "Acceso de uso".
 * El permiso se activa en Ajustes (fuera de la app): se re-chequea al volver.
 *
 * @param isUsageAccessGranted seam de tests: por defecto consulta el permiso real.
 */
@Composable
fun App(isUsageAccessGranted: (Context) -> Boolean = UsageAccess::isGranted) {
    val context = LocalContext.current
    val checkGranted by rememberUpdatedState(isUsageAccessGranted)
    var hasUsageAccess by remember { mutableStateOf(checkGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = checkGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasUsageAccess) {
        FocoNavHost()
    } else {
        OnboardingScreen()
    }
}
