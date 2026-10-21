package com.foco.app.domain

import java.time.LocalDate

/** A stop in the home's day ring: one day or the week summary. */
sealed interface DayStop {
    data class Day(
        val date: LocalDate,
    ) : DayStop

    data object Week : DayStop
}

/** The forward order: today, week, oldest offered day, ... , yesterday. */
fun dayRing(
    today: LocalDate,
    storedDays: Set<LocalDate>,
): List<DayStop> =
    buildList {
        add(DayStop.Day(today))
        add(DayStop.Week)
        // The detail's seven-day window bounds what the ring offers: the guard day is stored, not shown.
        val offered = availableDays(today, storedDays)
        // Today is the live head and is already added above; a stored marker for it must never
        // append a second position (otherwise forward from today is a silent no-op).
        storedDays.filter { it in offered && it != today }.sorted().forEach { add(DayStop.Day(it)) }
    }

/** One step around the ring; a vanished current position falls back to the ring's first stop. */
fun DayStop.step(
    ring: List<DayStop>,
    forward: Boolean,
): DayStop {
    if (ring.isEmpty()) return this
    val index = ring.indexOf(this)
    if (index < 0) return ring.first()
    val next = if (forward) (index + 1) % ring.size else (index - 1 + ring.size) % ring.size
    return ring[next]
}

/** Stable key for saveable UI state; dates are ISO-8601. */
fun DayStop.key(): String =
    when (this) {
        DayStop.Week -> "week"
        is DayStop.Day -> "day:$date"
    }

/** Parses a [key] back to a stop; null when it is not a ring key. */
fun dayStopFromKey(key: String): DayStop? =
    when {
        key == "week" -> DayStop.Week
        key.startsWith("day:") ->
            runCatching {
                DayStop.Day(LocalDate.parse(key.removePrefix("day:")))
            }.getOrNull()
        else -> null
    }
