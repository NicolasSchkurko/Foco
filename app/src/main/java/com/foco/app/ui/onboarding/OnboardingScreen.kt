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
import com.foco.app.R
import com.foco.app.data.UsageAccess

/**
 * Shown while the app has no "Usage access": what to enable, and the way to Settings. The Scaffold
 * shell is what gives the text the theme's content colour over the themed window.
 */
@Composable
fun OnboardingScreen() {
    val context = LocalContext.current

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Foco",
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_explanation),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Button(onClick = { context.startActivity(UsageAccess.settingsIntent()) }) {
                Text(stringResource(R.string.onboarding_open_settings))
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.onboarding_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
