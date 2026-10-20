package com.foco.app.domain

import java.time.LocalDate

/** One step or an explicit jump, pushing the previous position for the back action. */
fun DayRingState.step(ring: List<DayStop>, forward: Boolean): DayRingState {
    val next = current.step(ring, forward)
    return if (next == current) this else copy(current = next, history = history + current)
}

/** A bar tap is a shortcut: the same push as a step, with no parent-child edge. */
fun DayRingState.moveTo(target: DayStop, ring: List<DayStop>): DayRingState =
    if (target == current || target !in ring) this else copy(current = target, history = history + current)

/** Pops one visited position; null when there is nothing behind the current one. */
fun DayRingState.back(): DayRingState? =
    if (history.isEmpty()) null else copy(current = history.last(), history = history.dropLast(1))

/** Keeps the history honest when the store drops a day; a vanished current position falls back to today. */
fun DayRingState.retain(ring: List<DayStop>, today: LocalDate): DayRingState =
    DayRingState(
        current = if (current in ring) current else DayStop.Day(today),
        history = history.filter { it in ring }
    )
