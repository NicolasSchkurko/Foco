package com.foco.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLimitAlertNotifierTest {
    @Test
    fun distinctPackagesGetDistinctIds() {
        val a = notificationId("com.example.alpha")
        val b = notificationId("com.example.beta")
        assertNotEquals("different packages must not share an id", a, b)
    }

    @Test
    fun theSamePackageIsStable() {
        val a = notificationId("com.example.alpha")
        val b = notificationId("com.example.alpha")
        assertEquals(a, b)
    }

    @Test
    fun theIdIsNonNegative() {
        val id = notificationId("com.example.alpha")
        assertTrue(id >= 0)
    }
}
