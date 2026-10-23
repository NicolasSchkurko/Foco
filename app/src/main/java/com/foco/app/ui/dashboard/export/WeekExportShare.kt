package com.foco.app.ui.dashboard.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate

/** Writes the export as a transient cache file and answers the read-only URI that shares it. */
fun writeWeekExport(
    context: Context,
    json: String,
    weekEnd: LocalDate,
): Uri {
    val directory = File(context.cacheDir, "week-exports").apply { mkdirs() }
    val file = File(directory, "foco-week-$weekEnd.json")
    file.writeText(json, Charsets.UTF_8)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** The share intent: a JSON attachment the receiver may read through the granted URI. */
fun weekExportShareIntent(uri: Uri): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
