package com.foco.app.ui.limits

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.foco.app.domain.limits.BlockReason
import kotlinx.coroutines.isActive

/**
 * The full-screen block screen: names the app, states the reason and counts 3, 2, 1 down on the
 * frame clock, then calls [onFinished]. Resource: the accessibility overlay window.
 */
@Composable
fun AppLimitBlockScreen(
    appName: String,
    onFinished: () -> Unit,
    seconds: Int = 3,
    reason: BlockReason = BlockReason.LIMIT,
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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text =
                        stringResource(
                            if (reason == BlockReason.OUTSIDE_WINDOW) {
                                R.string.window_block_title
                            } else {
                                R.string.limit_block_title
                            },
                        ),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text =
                        stringResource(
                            if (reason == BlockReason.OUTSIDE_WINDOW) {
                                R.string.window_block_message
                            } else {
                                R.string.limit_block_message
                            },
                            appName,
                        ),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(32.dp))
                Text(
                    text = stringResource(R.string.limit_block_countdown, remaining),
                    style = MaterialTheme.typography.displayLarge,
                )
            }
        }
    }
}
