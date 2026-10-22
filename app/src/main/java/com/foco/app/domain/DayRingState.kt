package com.foco.app.domain

/** The current ring stop and the positions the user came through, oldest first. */
data class DayRingState(
    val current: DayStop,
    val history: List<DayStop> = emptyList(),
)

/** One step or an explicit jump, pushing the previous position for the back action. */
fun DayRingState.step(
    ring: List<DayStop>,
    forward: Boolean,
): DayRingState {
    val next = current.step(ring, forward)
    return if (next == current) this else copy(current = next, history = history + current)
}

/** A bar tap is a shortcut: the same push as a step, with no parent-child edge. */
fun DayRingState.moveTo(
    target: DayStop,
    ring: List<DayStop>,
): DayRingState = if (target == current || target !in ring) this else copy(current = target, history = history + current)

/** Pops one visited position; null when there is nothing behind the current one. */
fun DayRingState.back(): DayRingState? = if (history.isEmpty()) null else copy(current = history.last(), history = history.dropLast(1))

/**
 * Keeps the history honest when the store drops a day: a vanished current position falls back
 * to [fallback], and the history keeps only the positions still offered or allowed.
 */
fun DayRingState.retain(
    ring: List<DayStop>,
    isAllowed: (DayStop) -> Boolean,
    fallback: DayStop,
): DayRingState =
    DayRingState(
        current = if (current in ring || isAllowed(current)) current else fallback,
        history = history.filter { it in ring || isAllowed(it) },
    )
