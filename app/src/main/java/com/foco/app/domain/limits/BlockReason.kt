package com.foco.app.domain.limits

/** Why the accessibility host is showing the block overlay. */
enum class BlockReason {
    /** The daily minute budget is spent. */
    LIMIT,

    /** The current local time is outside the app's recurring window. */
    OUTSIDE_WINDOW,
}
