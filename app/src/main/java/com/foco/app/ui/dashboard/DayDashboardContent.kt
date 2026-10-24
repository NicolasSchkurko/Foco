package com.foco.app.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.ui.DetailTarget
import com.foco.app.ui.common.apps.AppUsageRow
import com.foco.app.ui.common.charts.HourChart
import com.foco.app.ui.common.layout.EmptyState
import com.foco.app.ui.formatTimeOfDay
import com.foco.app.ui.metrics.day.DayMetrics
import java.time.LocalDate

/**
 * Renders one day's dashboard content: the empty state if no usage entries exist, or the hourly
 * chart and the list of app usage rows.
 */
@Composable
fun DayDashboardContent(
    metrics: DayMetrics,
    date: LocalDate,
    today: LocalDate,
    onExclude: (String) -> Unit,
    onAppClick: (DetailTarget) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (metrics.entries.isEmpty()) {
            EmptyState(
                text =
                    if (date == today) {
                        stringResource(R.string.dashboard_empty)
                    } else {
                        stringResource(R.string.day_empty)
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            )
        } else {
            HourChart(metrics.hourly)
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.dashboard_apps),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                items(metrics.entries, key = { it.packageName }) { entry ->
                    AppUsageRow(
                        packageName = entry.packageName,
                        millis = entry.totalMillis,
                        barReferenceMillis = metrics.totalMillis,
                        caption =
                            stringResource(
                                R.string.row_last_used,
                                formatTimeOfDay(entry.lastUsedAt),
                            ),
                        onClick = { onAppClick(DetailTarget(entry.packageName, date)) },
                        onLongClick = { onExclude(entry.packageName) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}
