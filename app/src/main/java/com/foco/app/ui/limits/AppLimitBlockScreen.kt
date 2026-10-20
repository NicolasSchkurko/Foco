package com.foco.app.ui.limits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foco.app.R
import kotlinx.coroutines.isActive

/**
 * The full-screen limit screen: names the app, states the daily limit and counts 3, 2, 1 down
 * on the frame clock, then calls [onFinished]. Resource: the accessibility overlay window.
 */
@Composable
fun AppLimitBlockScreen(
    appName: String,
    onFinished: () -> Unit,
    seconds: Int = 3
) {
    var remaining by remember(seconds) { mutableIntStateOf(seconds) }

    LaunchedEffect(seconds) {
        val start = withFrameMillis { it }
        while (isActive) {
            val elapsed = withFrameMillis { it - start }
            val left = seconds - (elapsed / 1_000).toInt()
            if (left <= 0) {
                onFinished()
                return@LaunchedEffect
            }
            remaining = left
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.limit_block_title),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.limit_block_message, appName),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.limit_block_countdown, remaining),
                style = MaterialTheme.typography.displayLarge
            )
        }
    }
}
