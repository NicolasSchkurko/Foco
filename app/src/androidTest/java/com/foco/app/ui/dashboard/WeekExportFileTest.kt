package com.foco.app.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.foco.app.domain.buildWeekExport
import com.foco.app.domain.weekExportJson
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Category: export file (API 36). The export is a real cache file behind a read-only provider, shared
 * as a JSON attachment.
 */
@RunWith(AndroidJUnit4::class)
class WeekExportFileTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theExportIsACacheFileSharedAsJson() {
        val export = buildWeekExport(
            weekStart = LocalDate.of(2026, 10, 13),
            weekEnd = LocalDate.of(2026, 10, 19),
            storedTotalsByDay = emptyMap(),
            sessionsByDay = emptyMap(),
            todaySessions = emptyList(),
            labels = { it },
            zone = ZoneId.of("UTC")
        )

        val uri = writeWeekExport(context, weekExportJson(export), LocalDate.of(2026, 10, 19))

        assertEquals("content", uri.scheme)
        assertEquals("com.foco.app.fileprovider", uri.authority)
        val file = File(context.cacheDir, "week-exports/foco-week-2026-10-19.json")
        assertTrue("missing file: ${file.absolutePath}", file.exists())
        assertTrue("unexpected content: ${file.readText()}", file.readText().contains("2026-10-13"))

        val intent = weekExportShareIntent(uri)
        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/json", intent.type)
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))
        assertTrue(
            "no read grant in ${intent.flags}",
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0
        )
    }
}
