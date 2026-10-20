@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.foco.app.domain

import com.foco.app.model.export.WeekExport
import kotlinx.serialization.json.Json

private val prettyJson =
    Json {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

/** The export as the JSON document the share sheet carries, two-space indented. */
fun weekExportJson(export: WeekExport): String = prettyJson.encodeToString(WeekExport.serializer(), export)
