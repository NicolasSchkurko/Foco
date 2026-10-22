package com.foco.app.ui

import com.foco.app.ui.onboarding.OnboardingStep

/** What the app shows before the home: a permission step, the startup load or the home. */
sealed interface EntryState {
    /** The startup consolidation is running. */
    data object Loading : EntryState

    /** A permission is missing: this step asks for it. */
    data class Onboarding(
        val step: OnboardingStep,
    ) : EntryState

    /** Everything is granted and the startup load is done. */
    data object Home : EntryState
}
