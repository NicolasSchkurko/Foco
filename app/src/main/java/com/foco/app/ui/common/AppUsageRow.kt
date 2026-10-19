package com.foco.app.ui.common

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foco.app.ui.formatDuration

/**
 * One app in a ranking: icon, label, its share of [maxMillis] as a bar, its time and a [caption]
 * under it — the screen says what fits (when it was last used, or how many days it was used).
 */
@Composable
fun AppUsageRow(
    packageName: String,
    millis: Long,
    maxMillis: Long,
    caption: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIcon(packageName = packageName)
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appLabel(packageName),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            val fraction = if (maxMillis > 0) millis.toFloat() / maxMillis else 0f
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatDuration(millis),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
