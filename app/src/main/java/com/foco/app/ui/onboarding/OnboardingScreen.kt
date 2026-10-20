package com.foco.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.app.AccessibilityAccess
import com.foco.app.R
import com.foco.app.data.UsageAccess

/**
 * Shown while a required permission is missing: usage access first, then the accessibility
 * service that enforcement needs. The Scaffold gives the text the theme's content colour.
 */
@Composable
fun OnboardingScreen(accessibility: Boolean = false) {
    val context = LocalContext.current
    val explanation =
        if (accessibility) {
            stringResource(R.string.onboarding_accessibility_explanation)
        } else {
            stringResource(R.string.onboarding_explanation)
        }
    val action =
        if (accessibility) {
            stringResource(R.string.onboarding_accessibility_open_settings)
        } else {
            stringResource(R.string.onboarding_open_settings)
        }
    val hint =
        if (accessibility) {
            stringResource(R.string.onboarding_accessibility_hint)
        } else {
            stringResource(R.string.onboarding_hint)
        }
    val intent =
        if (accessibility) {
            AccessibilityAccess.settingsIntent()
        } else {
            UsageAccess.settingsIntent()
        }

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Foco",
                style = MaterialTheme.typography.displaySmall,
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Button(onClick = { context.startActivity(intent) }) {
                Text(action)
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
