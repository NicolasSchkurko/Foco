package com.foco.app.ui.dashboard

import com.foco.app.domain.DayRingState
import com.foco.app.domain.DayStop
import com.foco.app.domain.back
import com.foco.app.domain.moveTo
import com.foco.app.domain.retain
import com.foco.app.domain.step
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

/** The home's position, typed history and motion; no Android or Compose. */
class DashboardState private constructor(
    initial: Snapshot,
) {
    data class Snapshot(
        val ring: DayRingState,
        val shown: DayStop,
        val slideDirection: Int,
        val softMotion: Boolean,
    )

    private val _snapshot = MutableStateFlow(initial)
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()
    val value: Snapshot get() = _snapshot.value

    constructor(initial: DayStop) : this(Snapshot(DayRingState(initial), initial, 1, false))

    fun moveTo(
        target: DayStop,
        ring: List<DayStop>,
        isShortcut: Boolean,
        direction: Int? = null,
        soft: Boolean = false,
    ): Boolean {
        val current = value
        if (target == current.ring.current) return false
        if (!isShortcut && target !in ring) return false
        val from = ring.indexOf(current.ring.current)
        val to = ring.indexOf(target)
        val nextDirection = direction ?: if (from >= 0 && to > from) 1 else -1
        val nextRing =
            if (target in ring) {
                current.ring.moveTo(target, ring)
            } else {
                current.ring.copy(
                    current = target,
                    history = current.ring.history + current.ring.current,
                )
            }
        set(current.copy(ring = nextRing, slideDirection = nextDirection, softMotion = soft))
        return true
    }

    fun moveStep(
        ring: List<DayStop>,
        forward: Boolean,
    ): Boolean {
        val current = value
        val next = current.ring.step(ring, forward)
        if (next == current.ring) return false
        set(
            current.copy(
                ring = next,
                slideDirection = if (forward) 1 else -1,
                softMotion = true,
            ),
        )
        return true
    }

    fun goBack(): Boolean {
        val current = value
        val next = current.ring.back() ?: return false
        set(current.copy(ring = next, slideDirection = -1, softMotion = false))
        return true
    }

    /**
     * Keeps the history honest when the store drops a day: a vanished current position falls back
     * to today. The domain owns the rule; the survivors are the ring's plus the allowed ones.
     */
    fun retain(
        ring: List<DayStop>,
        isAllowed: (DayStop) -> Boolean,
        today: LocalDate,
    ) {
        val current = value
        val keep =
            ring +
                current.ring.history.filter(isAllowed) +
                listOfNotNull(current.ring.current.takeIf(isAllowed))
        val nextRing = current.ring.retain(keep, today)
        val isPresent = current.ring.current in keep
        set(
            current.copy(
                ring = nextRing,
                shown = if (isPresent) current.shown else DayStop.Day(today),
            ),
        )
    }

    fun showCurrent() {
        val current = value
        if (current.shown != current.ring.current) set(current.copy(shown = current.ring.current))
    }

    private fun set(next: Snapshot) {
        if (next != _snapshot.value) _snapshot.value = next
    }

    companion object {
        fun restore(
            current: DayStop,
            shown: DayStop,
            history: List<DayStop>,
            slideDirection: Int,
            softMotion: Boolean,
        ): DashboardState =
            DashboardState(
                Snapshot(
                    ring = DayRingState(current, history),
                    shown = shown,
                    slideDirection = slideDirection,
                    softMotion = softMotion,
                ),
            )
    }
}
