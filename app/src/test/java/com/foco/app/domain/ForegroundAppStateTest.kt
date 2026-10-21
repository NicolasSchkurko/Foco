package com.foco.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundAppStateTest {
    private val state =
        ForegroundAppState(
            selfPackage = "com.foco.app",
            ignoredPackages = setOf("com.android.systemui"),
            inputMethodPackages = setOf("com.google.android.inputmethod.latin"),
        )

    @Test
    fun `a normal window event tracks that app`() {
        assertEquals("com.youtube", state.onWindowEvent("com.youtube"))
        assertEquals("com.youtube", state.current)
    }

    @Test
    fun `foco coming to the front clears the tracked app`() {
        state.onWindowEvent("com.youtube")

        assertNull(state.onWindowEvent("com.foco.app"))
        assertNull(state.current)
    }

    @Test
    fun `a stale monitor request is rejected once foco is in front`() {
        state.onWindowEvent("com.youtube")
        state.onWindowEvent("com.foco.app")

        assertFalse(state.accepts("com.youtube"))
    }

    @Test
    fun `a request is accepted while its app is in front`() {
        state.onWindowEvent("com.youtube")

        assertTrue(state.accepts("com.youtube"))
    }

    @Test
    fun `system ui and the input method keep the app in use`() {
        state.onWindowEvent("com.youtube")

        assertNull(state.onWindowEvent("com.android.systemui"))
        assertNull(state.onWindowEvent("com.google.android.inputmethod.latin"))
        assertEquals("com.youtube", state.current)
    }

    @Test
    fun `a null package changes nothing`() {
        state.onWindowEvent("com.youtube")

        assertNull(state.onWindowEvent(null))
        assertEquals("com.youtube", state.current)
    }

    @Test
    fun `clear forgets the app`() {
        state.onWindowEvent("com.youtube")

        state.clear()

        assertNull(state.current)
        assertFalse(state.accepts("com.youtube"))
    }
}
