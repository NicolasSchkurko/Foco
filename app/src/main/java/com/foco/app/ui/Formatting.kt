package com.foco.app.ui

import androidx.appcompat.app.AppCompatDelegate
import com.foco.app.model.LanguageOption
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 3 h 24 min / 12 min / 40 s depending on magnitude. */
fun formatDuration(millis: Long): String {
    if (millis < 60_000) return "${millis / 1000} s"
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours h $minutes min" else "$minutes min"
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun formatTimeOfDay(
    epochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = Instant.ofEpochMilli(epochMillis).atZone(zone).format(timeFormatter)

/** The app's active language — the locale its strings resolve in. */
fun appLocale(): Locale {
    val requested =
        try {
            val locales = AppCompatDelegate.getApplicationLocales()
            if (!locales.isEmpty) locales.get(0) ?: Locale.getDefault() else Locale.getDefault()
        } catch (_: Throwable) {
            Locale.getDefault()
        }
    val shipped = LanguageOption.entries.filter { it != LanguageOption.AUTO }.map { it.wire }
    return if (requested.language in shipped) Locale.forLanguageTag(requested.language) else Locale.ENGLISH
}

/** The week bar's label: the app language's short weekday (e.g. Mon / lun). */
fun formatWeekday(
    date: LocalDate,
    locale: Locale = appLocale(),
): String = DateTimeFormatter.ofPattern("EEE", locale).format(date)

/**
 * Day selector label: today/yesterday use the active language's; the rest, the abbreviated date
 * according to the app's active language.
 */
fun formatDayLabel(
    date: LocalDate,
    today: LocalDate,
    todayLabel: String,
    yesterdayLabel: String,
    locale: Locale = appLocale(),
): String =
    when (date) {
        today -> todayLabel
        today.minusDays(1) -> yesterdayLabel
        else -> DateTimeFormatter.ofPattern("EEE d MMM", locale).format(date)
    }
