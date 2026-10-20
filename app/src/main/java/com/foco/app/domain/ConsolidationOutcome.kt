package com.foco.app.domain

/** What one consolidation run ended as: done, or failed with the cause the caller reports. */
sealed interface ConsolidationOutcome {
    data object Done : ConsolidationOutcome

    data class Failed(
        val cause: Throwable,
    ) : ConsolidationOutcome
}
