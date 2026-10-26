package com.foco.app.ui.common.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.foco.app.R
import com.foco.app.data.db.history.dailyaggregate.DailyAggregate
import com.foco.app.model.day.DayData
import com.foco.app.ui.common.apps.AppUsageRow
import com.foco.app.ui.common.layout.EmptyState
import com.foco.app.ui.common.time.rememberAppLocale
import com.foco.app.ui.common.time.rememberDayLabel
import com.foco.app.ui.formatDuration
import com.foco.app.ui.formatWeekday
import com.foco.app.ui.metrics.week.WeekDayBar
import com.foco.app.ui.metrics.week.deriveWeekMetrics
import java.time.LocalDate
import java.util.Locale

/**
 * The week as one home position: the bars and the ranking over the totals and the live day the home
 * already read. It renders only; the home owns the header, the reads and the loading placeholder.
 */
@Composable
fun WeekContent(
    today: LocalDate,
    totals: List<DailyAggregate>,
    todayData: DayData,
    excludedPackages: Set<String>,
    onDayClick: (LocalDate) -> Unit = {},
    onAppClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val metrics =
        remember(totals, todayData, excludedPackages, today) {
            deriveWeekMetrics(
                storedTotals = totals,
                todayDate = today,
                today = todayData,
                excludedPackages = excludedPackages,
            )
        }
    // The locale and the day labels are read once for the whole week, not once per bar.
    val locale = rememberAppLocale()
    val dayLabel = rememberDayLabel(today, locale)

    Column(modifier = modifier.fillMaxSize()) {
        WeekBars(
            bars = metrics.bars,
            dayLabel = dayLabel,
            locale = locale,
            onDayClick = onDayClick,
        )
        Spacer(Modifier.height(24.dp))

        if (metrics.ranking.isEmpty()) {
            EmptyState(
                text = stringResource(R.string.week_empty),
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } else {
            Text(
                text = stringResource(R.string.dashboard_apps),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(4.dp))
            val maxAppMillis = metrics.ranking.maxOf { it.totalMillis }
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
            ) {
                items(metrics.ranking, key = { it.packageName }) { app ->
                    AppUsageRow(
                        packageName = app.packageName,
                        millis = app.totalMillis,
                        barReferenceMillis = maxAppMillis,
                        caption =
                            pluralStringResource(
                                R.plurals.week_days_used,
                                app.daysUsed,
                                app.daysUsed,
                            ),
                        onClick = { onAppClick(app.packageName) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

/** The seven bars in one row, oldest first; the tallest one fills the track. */
@Composable
private fun WeekBars(
    bars: List<WeekDayBar>,
    dayLabel: (LocalDate) -> String,
    locale: Locale,
    onDayClick: (LocalDate) -> Unit,
) {
    val maxMillis =
        bars
            .filter { it.state != WeekDayBar.State.Absent }
            .maxOfOrNull { it.totalMillis }
            ?: 0L

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            WeekBar(
                bar = bar,
                maxMillis = maxMillis,
                dayLabel = dayLabel,
                locale = locale,
                onClick = { onDayClick(bar.date) },
            )
        }
    }
}

/**
 * One day: its track with the bar bottom-aligned (a short marker when the day is absent, so the
 * gap never reads as a zero) and its weekday and day of month below. One tappable, readable node.
 */
@Composable
private fun WeekBar(
    bar: WeekDayBar,
    maxMillis: Long,
    dayLabel: (LocalDate) -> String,
    locale: Locale,
    onClick: () -> Unit,
) {
    val description =
        if (bar.state == WeekDayBar.State.Absent) {
            stringResource(R.string.week_bar_absent, dayLabel(bar.date))
        } else {
            stringResource(R.string.week_bar, dayLabel(bar.date), formatDuration(bar.totalMillis))
        }
    val absent = bar.state == WeekDayBar.State.Absent
    val fraction = if (maxMillis > 0) bar.totalMillis.toFloat() / maxMillis else 0f

    Column(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .semantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .width(28.dp)
                    .height(120.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(if (absent) 4.dp else maxOf(4.dp, 120.dp * fraction))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(
                            if (absent) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        ),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatWeekday(bar.date, locale),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = bar.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
