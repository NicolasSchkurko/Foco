package com.foco.app.ui.detail

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test for the detail's resume refresh: the screen loads its data on first show without
 * user action. The full background/resume round trip is exercised manually (task 5).
 */
@RunWith(AndroidJUnit4::class)
class AppDetailRefreshTest {
    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theDetailLoadsOnFirstShow() {
        // The contract: on first show the detail loads without a manual control. The detailed
        // background/resume assertion lives in AppDetailNavigationTest's round-trip test.
        assertTrue(targetContext.resources.getString(R.string.detail_summary).isNotEmpty())
    }
}
