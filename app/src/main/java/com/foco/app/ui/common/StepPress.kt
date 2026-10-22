package com.foco.app.ui.common

import kotlinx.coroutines.Job

/**
 * The state of a step button's press, outside the snapshot state: the repeat job and the steps
 * the held press already applied, so the click that ends the press does not step again.
 */
class StepPress {
    /** The job that repeats the step while the press is held. */
    var repeatJob: Job? = null

    /** The steps the held press applied. */
    var gestureSteps = 0

    /** The gesture steps the last click already accounted for. */
    var accountedSteps = 0

    /** A new press: the counters of the previous one are history. */
    fun begin() {
        gestureSteps = 0
        accountedSteps = 0
    }

    /** Stops the repeat: the press is over. */
    fun stopRepeat() {
        repeatJob?.cancel()
        repeatJob = null
    }
}
