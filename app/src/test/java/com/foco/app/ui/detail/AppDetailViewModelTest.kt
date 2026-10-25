package com.foco.app.ui.detail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AppDetailViewModelTest {
    private val fixedInstant = Instant.parse("2026-10-20T10:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
    private val today = LocalDate.now(clock)
    private val testScope = CoroutineScope(Dispatchers.Unconfined)
    private val testPackage = "com.example.app"

    @Test
    fun `initial state sets package name selectedDate and shownDate`() {
        val viewModel =
            AppDetailViewModel(
                packageName = testPackage,
                clock = clock,
                coroutineScope = testScope,
            )
        val state = viewModel.uiState.value

        assertEquals(testPackage, state.packageName)
        assertEquals(today, state.selectedDate)
        assertEquals(today, state.shownDate)
    }

    @Test
    fun `stepDay updates index and selected date`() {
        val viewModel =
            AppDetailViewModel(
                packageName = testPackage,
                clock = clock,
                coroutineScope = testScope,
            )

        viewModel.stepDay(1)
        val stateAfterStep = viewModel.uiState.value
        assertEquals(0, stateAfterStep.effectiveIndex)
        assertEquals(today, stateAfterStep.selectedDate)
    }
}
