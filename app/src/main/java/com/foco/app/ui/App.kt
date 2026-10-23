package com.foco.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.foco.app.AccessibilityAccess
import com.foco.app.R
import com.foco.app.data.consolidation.ConsolidationRunner
import com.foco.app.data.system.UsageAccess
import com.foco.app.ui.onboarding.OnboardingScreen
import com.foco.app.ui.onboarding.OnboardingStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Onboarding, the startup gate, or the home; both reads are the tests' seams. */
@Composable
fun App(
    isUsageAccessGranted: (Context) -> Boolean = UsageAccess::isGranted,
    isAccessibilityGranted: (Context) -> Boolean = AccessibilityAccess::isGranted,
    startupLoad: (suspend () -> Unit)? = null,
) {
    val context = LocalContext.current
    val checkGranted by rememberUpdatedState(isUsageAccessGranted)
    val checkAccessibility by rememberUpdatedState(isAccessibilityGranted)
    var hasUsageAccess by remember { mutableStateOf(checkGranted(context)) }
    var hasAccessibility by remember { mutableStateOf(checkAccessibility(context)) }
    var startupReady by remember { mutableStateOf(false) }

    val loadStartup by rememberUpdatedState(
        startupLoad ?: {
            ConsolidationRunner.forApp(context.applicationContext).run(LocalDate.now())
            Unit
        },
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    hasUsageAccess = checkGranted(context)
                    hasAccessibility = checkAccessibility(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasUsageAccess, hasAccessibility) {
        if (!hasUsageAccess || !hasAccessibility) {
            startupReady = false
            return@LaunchedEffect
        }
        startupReady = false
        try {
            withContext(Dispatchers.IO) { loadStartup() }
        } finally {
            startupReady = true
        }
    }

    // What the app shows: the permission step that is missing, the startup load, or the home.
    val entryState: EntryState =
        when {
            !hasUsageAccess -> EntryState.Onboarding(OnboardingStep.UsageAccess)
            !hasAccessibility -> EntryState.Onboarding(OnboardingStep.Accessibility)
            !startupReady -> EntryState.Loading
            else -> EntryState.Home
        }
    when (entryState) {
        is EntryState.Onboarding -> OnboardingScreen(entryState.step)
        EntryState.Loading -> StartupLoadingScreen()
        EntryState.Home -> FocoNavHost()
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
