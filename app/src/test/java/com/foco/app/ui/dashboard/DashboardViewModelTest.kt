package com.foco.app.ui.dashboard

import com.foco.app.domain.ring.DayStop
import com.foco.app.model.day.DayData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class DashboardViewModelTest {
    private val fixedInstant = Instant.parse("2026-10-20T10:00:00Z")
    private val clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"))
    private val today = LocalDate.now(clock)
    private val testScope = CoroutineScope(Dispatchers.Unconfined)

    @Test
    fun `initial state sets default ring position to today`() {
        val viewModel =
            DashboardViewModel(
                clock = clock,
                loadDayOverride = { DayData(emptyList(), emptyList()) },
                coroutineScope = testScope,
            )
        val state = viewModel.uiState.value

        assertEquals(DayStop.Day(today), state.shown)
        assertEquals(DayStop.Day(today), state.requested)
        assertEquals(today, state.today)
    }

    @Test
    fun `moveStep forward and backward updates current position`() {
        val viewModel =
            DashboardViewModel(
                clock = clock,
                loadDayOverride = { DayData(emptyList(), emptyList()) },
                coroutineScope = testScope,
            )

        viewModel.moveStep(forward = true)
        val stateAfterNext = viewModel.uiState.value
        assertEquals(DayStop.Week, stateAfterNext.requested)
        assertEquals(1, stateAfterNext.slideDirection)
        assertTrue(stateAfterNext.softMotion)

        viewModel.moveStep(forward = false)
        val stateAfterPrev = viewModel.uiState.value
        assertEquals(DayStop.Day(today), stateAfterPrev.requested)
        assertEquals(-1, stateAfterPrev.slideDirection)
    }

    @Test
    fun `moveTo updates position and history`() {
        val viewModel =
            DashboardViewModel(
                clock = clock,
                loadDayOverride = { DayData(emptyList(), emptyList()) },
                coroutineScope = testScope,
            )

        viewModel.moveTo(DayStop.Week)
        assertEquals(DayStop.Week, viewModel.uiState.value.requested)
        assertEquals(listOf(DayStop.Day(today)), viewModel.uiState.value.ringState.history)

        assertTrue(viewModel.goBack())
        assertEquals(DayStop.Day(today), viewModel.uiState.value.requested)
    }

    @Test
    fun `exportWeekJson delegates to loadExportOverride`() =
        runBlocking {
            val expectedJson = "{\"week\":\"2026-10-20\"}"
            val viewModel =
                DashboardViewModel(
                    clock = clock,
                    loadExportOverride = { expectedJson },
                    coroutineScope = testScope,
                )

            val result = viewModel.exportWeekJson()
            assertEquals(expectedJson, result)
        }
}
