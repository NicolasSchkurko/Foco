package com.foco.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.foco.app.R
import com.foco.app.ui.appLocale
import com.foco.app.ui.formatDayLabel
import java.time.LocalDate
import java.util.Locale

/** The app's locale, read once per composition: the labels and the week day follow it. */
@Composable
fun rememberAppLocale(): Locale = remember { appLocale() }

/** The label of a day: today/yesterday in the app's language, else the abbreviated date. */
@Composable
fun rememberDayLabel(
    today: LocalDate,
    locale: Locale,
): (LocalDate) -> String {
    val todayLabel = stringResource(R.string.day_today)
    val yesterdayLabel = stringResource(R.string.day_yesterday)
    return remember(locale, todayLabel, yesterdayLabel, today) {
        { date -> formatDayLabel(date, today, todayLabel, yesterdayLabel, locale) }
    }
}
