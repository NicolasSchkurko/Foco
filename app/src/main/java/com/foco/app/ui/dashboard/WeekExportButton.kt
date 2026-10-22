package com.foco.app.ui.dashboard

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.foco.app.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * The week's export: the button that writes the JSON file and shares it. The file is written off
 * the main thread; the share sheet opens on it.
 */
@Composable
fun WeekExportButton(
    exportJson: suspend () -> String,
    weekEnd: LocalDate,
    onFailed: suspend () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    val exportLabel = stringResource(R.string.week_export)

    IconButton(
        enabled = !exporting,
        onClick = {
            exporting = true
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) { exportJson() }
                    val uri = withContext(Dispatchers.IO) { writeWeekExport(context, json, weekEnd) }
                    context.startActivity(
                        Intent.createChooser(weekExportShareIntent(uri), null),
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    onFailed()
                } finally {
                    exporting = false
                }
            }
        },
    ) {
        Icon(
            imageVector = Icons.Filled.Share,
            contentDescription = exportLabel,
        )
    }
}
