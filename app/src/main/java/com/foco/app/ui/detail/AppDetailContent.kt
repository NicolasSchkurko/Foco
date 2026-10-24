package com.foco.app.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.domain.limits.AppWindow
import com.foco.app.ui.common.charts.HourChart
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatMinuteOfDay24
import com.foco.app.ui.formatTimeOfDay
import com.foco.app.ui.metrics.detail.AppDayDetail

/**
 * Renders the detail content for an app: the limits preview section, hourly breakdown,
 * day summary, and session list.
 */
@Composable
fun AppDetailContent(
    detail: AppDayDetail,
    limitMinutes: Int?,
    window: AppWindow?,
    alertOn: Boolean,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openLimitsLabel = stringResource(R.string.detail_limits_configure)

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = openLimitsLabel,
                        role = Role.Button,
                        onClick = onConfigure,
                    ).padding(vertical = 4.dp)
                    .testTag(LIMITS_PREVIEW_TAG),
        ) {

        HourChart(detail.hourly)

        Spacer(Modifier.height(16.dp))


        Text(
            text = stringResource(R.string.detail_summary),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))

        val summary =
            listOf(
                stringResource(R.string.detail_total) to formatDuration(detail.totalMillis),
                stringResource(R.string.detail_sessions) to detail.sessionCount.toString(),
                stringResource(R.string.detail_first) to formatTimeOfDay(detail.firstStart),
                stringResource(R.string.detail_last) to formatTimeOfDay(detail.lastEnd),
                stringResource(R.string.detail_average) to formatDuration(detail.averageMillis),
                stringResource(R.string.detail_longest) to formatDuration(detail.longestMillis),
            )
        summary.forEach { (label, value) ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.detail_limits_section),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        val preview =
                listOf(
                    stringResource(R.string.detail_limits_limit_label) to previewLimitLabel(limitMinutes),
                    stringResource(R.string.settings_window_label) to
                            (
                                    if (window ==
                                        null
                                    ) {
                                        stringResource(R.string.settings_window_none)
                                    } else {
                                        stringResource(
                                            R.string.settings_window_range,
                                            formatMinuteOfDay24(window.startMinute),
                                            formatMinuteOfDay24(window.endMinute),
                                        )
                                    }
                                    ),
                    stringResource(R.string.detail_limits_alerts_label) to
                            (
                                    if (alertOn) {
                                        stringResource(
                                            R.string.detail_limits_alerts_on,
                                        )
                                    } else {
                                        stringResource(R.string.detail_limits_alerts_off)
                                    }
                                    ),
                )
            preview.forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(text = value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.detail_sessions),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            items(detail.sessions) { session ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${formatTimeOfDay(session.start)} – ${formatTimeOfDay(session.end)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = formatDuration(session.end - session.start),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

/** The limit as the preview shows it: the same words the editor shows. */
@Composable
private fun previewLimitLabel(minutes: Int?): String =
    when (minutes) {
        null -> stringResource(R.string.settings_limits_value_none)
        0 -> stringResource(R.string.settings_limits_value_block_now)
        else -> stringResource(R.string.settings_limits_value_minutes, minutes)
    }
