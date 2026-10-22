package com.foco.app.ui

import java.time.LocalDate

/** Which app, and on which day: today when the day is absent. */
data class DetailTarget(
    val packageName: String,
    val date: LocalDate? = null,
)

/** The route of a detail: the day is a query argument when it is not today. */
fun detailRoute(target: DetailTarget): String =
    target.date?.let { "detail/${target.packageName}?date=$it" } ?: "detail/${target.packageName}"

/** The day of a route argument; absent or malformed dates mean today. */
fun parseRouteDate(raw: String?): LocalDate? = raw?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
