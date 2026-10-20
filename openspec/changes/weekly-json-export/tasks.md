# Tasks — weekly-json-export

- [ ] 1. Add `kotlin-serialization` to `gradle/libs.versions.toml` (`org.jetbrains.kotlin.plugin.serialization`, Kotlin 2.4.21) and `kotlinx-serialization-json` to the libraries (version from the 1.8.1 BOM); apply the plugin and add the runtime in `app/build.gradle.kts`. This build-file edit is sequenced after `nightly-consolidation` and `schema-drift-guard`.
- [ ] 2. Add the `@Serializable` export model, one type per file under `model/export/`: `WeekExport`, `ExportDay`, `ExportApp`, `ExportSession` (with `start`, `end` as ISO-8601 strings and `durationMillis`).
- [ ] 3. Write `WeekExportTest` first for the riskiest part: seven days including empty ones, app/session totals, stored-total fallback with `sessionsAvailable: false`, raw sub-second/excluded records, ISO offsets, deterministic app ordering, escaping, and a JSON encode/decode round trip.
- [ ] 4. Add the pure `buildWeekExport` grouping in `domain/` (it takes the window dates, sessions-by-day, stored-totals-by-day, labels and zone) and `weekExportJson` using `Json { prettyPrint = true; prettyPrintIndent = "  " }`; run the JVM test until green.
- [ ] 5. Add `WeekExportService` in `data/`: read `SessionHistoryStore.week`, `sessionsFor` for the previous six days, today's `UsageRepository.loadDay`, resolve labels through `AppPackageInfo`, and propagate read failures instead of returning a partial export.
- [ ] 6. Add the `FileProvider` entry and `file_provider_paths.xml`; add `writeWeekExport` (cache file) and `weekExportShareIntent`/`shareWeekExport` (`ACTION_SEND`, `application/json`, read grant) under `ui/dashboard/`.
- [ ] 7. Add the **Export JSON** text button to the home header while the Week stop is showing, with exporting state and error snackbar; keep a `loadExport` seam so the failure path is testable.
- [ ] 8. Add `WeekExportShareTest` for the cache file, `content://` URI, MIME type and intent flags; extend `WeekScreenTest` (the week position) for the export action and the failed-read error.
- [ ] 9. Run `gradlew.bat test`, then `gradlew.bat connectedDebugAndroidTest` on the Pixel 8; share the file and open it on a computer to verify the seven days and the session breakdown.
- [ ] 10. Re-run `openspec validate weekly-json-export --strict` and `node scripts/gates/build-board.mjs`.
