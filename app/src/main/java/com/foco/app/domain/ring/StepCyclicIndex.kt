package com.foco.app.domain.ring

/**
 * The next position of a cyclic list: [delta] of +1 moves forward, -1 moves back, and the walk
 * cycles (past the last it starts again, before the first it ends at the last).
 */
fun stepCyclicIndex(
    index: Int,
    size: Int,
    delta: Int,
): Int {
    if (size <= 0) return 0
    val safe = index.coerceIn(0, size - 1)
    return (((safe + delta) % size) + size) % size
}
