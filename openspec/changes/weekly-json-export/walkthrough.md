# Walkthrough — weekly-json-export (tech brief)

## Mechanism

The home shows a ring of positions; one of them is the **week**. When that position is showing, the
header gains a share action (`DashboardScreen.kt`, the `actions` block guarded by
`if (stop is DayStop.Week)`). Tapping it runs three steps and then hands the file to Android.

**1. Read the window.** `WeekExportService.export(today)` (`WeekExportService.kt:23-42`) reads the
same window the screen shows: `store.week(today)` (per-app totals for today-6 … today),
`store.sessionsFor(date)` for each of the six stored days, and `repository.loadToday()` for today.
A throw in any read propagates: nothing is written and the caller shows the error.

**2. Build the document.** `buildWeekExport(...)` (`WeekExportBuilder.kt:17-63`) is pure and takes no
Android type. For each day, oldest first, it unions the apps that have a stored total with the apps
that have raw sessions. An app with sessions takes its total **from the sessions** and sets
`sessionsAvailable = true`; an app with only a stored total keeps that total and sets
`sessionsAvailable = false`. No visibility or exclusion filter is applied — this is a record, not the
ranking. Timestamps become ISO-8601 with the local offset (`isoTimestamp`, `:66-67`) and the day and
app totals are sums (`:53-60`). `weekExportJson` (`WeekExportJson.kt:12-13`) encodes it with
`kotlinx.serialization` using `prettyPrint = true` and a two-space indent.

**3. Share it.** `writeWeekExport(context, json, weekEnd)` (`WeekExportShare.kt:11-17`) writes
`cacheDir/week-exports/foco-week-<weekEnd>.json` and returns the read-only `content://` URI that the
manifest's `FileProvider` (authority `${applicationId}.fileprovider`, paths from
`res/xml/file_provider_paths.xml`) exposes. `weekExportShareIntent(uri)` (`:19-25`) builds
`ACTION_SEND` + `application/json` + `EXTRA_STREAM` + `FLAG_GRANT_READ_URI_PERMISSION`, and the
dashboard starts it through `Intent.createChooser`.

**Failure path.** Any throw inside export goes to the `catch` in the click handler, which shows the
`week_export_error` snackbar and leaves `exporting` false; the file is only written **after** the
document is built, so a partial export cannot exist.

## Physical traceability

- **Where the file lives**: `cacheDir/week-exports/`, i.e. `/data/user/0/com.foco.app/cache/week-exports/`
  on device. The system may evict it; it is never backed up (`allowBackup="false"` from the
  foundation).
- **How it is exposed**: one `FileProvider` entry in `AndroidManifest.xml`
  (`androidx.core.content.FileProvider`, `exported="false"`, `grantUriPermissions="true"`, paths from
  `@xml/file_provider_paths` → `<cache-path name="week-exports" path="week-exports/">`). The URI is
  read-only and the grant is per-intent.
- **Who reads the data**: the same store and repository the screens use — `SessionHistoryStore.week`,
  `sessionsFor`, and `UsageRepository.loadToday`. No new database, no new table, no schema change.
- **Nothing leaves the device by itself**: the file is handed to the app the user picks in the share
  sheet; there is no network call, no storage permission, no manifest permission added.
- **Lifecycle**: the cache file is rewritten on every export (same name per `weekEnd`); leftovers are
  ordinary cache the system can delete.

## Limits and failure modes

- **A stored total without raw rows exports as a number with no sessions.** That is the honest
  fallback (`sessionsAvailable: false`), and it is what makes an old app survive after pruning.
- **The window is the screen's window**: seven days ending today. A day older than retention is not
  exported even if it once existed.
- **Today has no stored totals**: it comes from the live read, so its `apps` are exactly today's
  sessions.
- **One query per stored day** (six `sessionsFor` calls) plus one live read: a slow database makes the
  export slow, but the action shows no progress dialog — only the disabled icon while it runs.
- **A read failure shows an error, not a partial file**; the exported document is built in memory
  before anything is written.
- **The share target decides what happens next**: the app cannot know whether the receiver opens,
  sends or ignores the JSON.

## Touched APIs

- **`kotlinx.serialization`** plugin `org.jetbrains.kotlin.plugin.serialization` 2.4.21
  (`libs.versions.toml:54`, `app/build.gradle.kts:6`) and `kotlinx-serialization-json` 1.8.1
  (`libs.versions.toml:40`, `app/build.gradle.kts:74`) — `@Serializable` models, `Json.prettyPrint`,
  `prettyPrintIndent`.
- **`androidx.core.content.FileProvider`** + `<provider>` + `<cache-path>` — the read-only URI.
- **`Intent.ACTION_SEND`**, `EXTRA_STREAM`, `FLAG_GRANT_READ_URI_PERMISSION`, `Intent.createChooser` —
  the share sheet.
- **`Context.cacheDir`** — the transient file.
- **`java.time`** — `ISO_OFFSET_DATE_TIME` for the timestamps, `LocalDate` for the window.

## Implicit decisions

- **`WeekExportService` takes the `Context`** (instead of an injected store): `LayerRulesTest` sends a
  file without Android imports in `data/` to `domain/`, and this class is genuinely platform-coupled —
  it reads the platform reader and builds the store, like `SessionHistoryStore.forApp`.
- **`loadExport` is a seam on `DashboardScreen`** (a `suspend () -> String`), so the error path is
  testable without the platform; the production default builds the service.
- **The export ignores exclusions and the 1 s rule** on purpose: it is a record of what happened, not
  the ranking the screen shows.
- **Two instrumented classes instead of one**: a Compose test and a plain test in the same class
  crashed the instrumentation process on API 36; splitting them runs clean (`WeekExportFileTest` for
  the file/intent, `WeekExportShareTest` for the error UI).
- **The JSON is written by the library, not by hand**: escaping (quotes, backslashes, control
  characters) is the serializer's job and the JVM test round-trips a label that needs it.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. A day's app has a stored total but its raw rows were pruned. Describe what the export contains for
   that app, what `sessionsAvailable` says, and why the total is not zero.
   > **Answer (Nschkurko, 2026-10-19):** El total de esa app sale del total guardado como respaldo, no
   > de la suma de sesiones. Por eso `totalMillis` no es cero. Y `sessionsAvailable = false`
   > (`mine.isEmpty()`), con `sessions` vacío: el número viaja sin detalle.
2. The export fails halfway through reading the six stored days. Say what the user sees, whether a
   file exists afterwards, and which part of the code guarantees that.
   > **Answer (Nschkurko, 2026-10-19):** `WeekExportService.export()` solo lee y arma el documento en
   > memoria. El archivo lo escribe `writeWeekExport` (`WeekExportShare.kt:11`), que se llama después de
   > `export()` y de `weekExportJson`. Esa secuencia —construir todo → recién escribir— es la que
   > garantiza que no quede un archivo parcial. Si una lectura falla, no hay archivo nuevo (a lo sumo
   > uno completo anterior, porque el nombre es fijo) y el usuario ve el snackbar de error.
3. The document is shared. Name what makes the receiving app able to read a file from Foco's private
   cache, and what would happen if the manifest entry or the read-grant flag were missing.
   > **Answer (Nschkurko, 2026-10-19):** Sin la entrada del `FileProvider` en el manifest,
   > `FileProvider.getUriForFile` tira excepción, no hay URI que compartir y el export falla con el
   > snackbar de error. Sin el flag de grant (`FLAG_GRANT_READ_URI_PERMISSION`), el archivo se escribe y
   > el share sheet se abre, pero la app receptora no puede leer el `content://`; falla con permiso
   > denegado al abrirlo.
