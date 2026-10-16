package com.foco.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
 */
@Composable
fun App() {
    val context = LocalContext.current
    var hasUsageAccess by remember { mutableStateOf(UsageAccess.isGranted(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = UsageAccess.isGranted(context)
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
