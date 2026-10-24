package com.foco.app.ui.common.layout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

/**
 * A reusable step selector row with left/right navigation controls and central text.
 * Used for stepping through positions/days in screens like Dashboard and App Detail.
 */
@Composable
fun StepSelectorRow(
    text: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    previousEnabled: Boolean = true,
    nextEnabled: Boolean = true,
    previousContentDescription: String? = null,
    nextContentDescription: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    textModifier: Modifier = Modifier,
    useArrowIcons: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = previousEnabled,
            modifier =
                if (previousContentDescription != null) {
                    Modifier.semantics { contentDescription = previousContentDescription }
                } else {
                    Modifier
                },
        ) {
            if (useArrowIcons) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                )
            } else {
                Text("‹", style = MaterialTheme.typography.titleLarge)
            }
        }

        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = textModifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }

        IconButton(
            onClick = onNext,
            enabled = nextEnabled,
            modifier =
                if (nextContentDescription != null) {
                    Modifier.semantics { contentDescription = nextContentDescription }
                } else {
                    Modifier
                },
        ) {
            if (useArrowIcons) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                )
            } else {
                Text("›", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
