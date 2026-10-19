# Walkthrough — usage-history-foundation (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them.

## Mechanism

`UsageStatsManager` only serves roughly the last seven days, so the app now copies each finished day
into its own SQLite database while the platform still remembers it. There is no Application
subclass: `MainActivity.onCreate` calls `consolidateHistory()` (`MainActivity.kt:56`) after the
theme is applied, and that call is the whole trigger.

The chain, once per process:

1. `consolidateHistory()` guards with a process-wide `AtomicBoolean` so a second `onCreate`
   (rotation, re-entry) cannot start a parallel run (`MainActivity.kt:78-79,96`), then launches on
   `Dispatchers.IO` inside `lifecycleScope` (`MainActivity.kt:81`).
2. `SessionHistoryStore.forApp(...)` (`SessionHistoryStore.kt:64`) memoises a single Room database
   in the companion (`SessionHistoryStore.kt:61-70`) — file `foco.db`, built with the application
   context, never opened twice.
3. `consolidate(today)` (`SessionHistoryStore.kt:28`) asks `DailyAggregateDao.storedDays()` which
   days already have totals, takes `availableDays(today)` (pure, existing), drops the first element
   — **today, whose data is not final** — and keeps only the days without totals, oldest first
   (`SessionHistoryStore.kt:30`).
4. Per day, `consolidateDay` reads the platform through `UsageRepository.loadDay`, returns early on
   an empty day, then writes the **raw sessions first** and the **per-app totals second**
   (`SessionHistoryStore.kt:39-44`). The order is deliberate: a crash between the two writes leaves
   no totals, so the day stays pending and the next start retries it; the composite key makes that
   retry a `REPLACE`, not a duplicate.
5. Finally `pruneBefore(rawCutoff(today))` deletes raw rows whose `end` is before the start of
   `today - 6 days` in the system zone (`SessionHistoryStore.kt:32,53-54`). Totals live in another
   table and are never touched by the prune.

The schema is two tables with composite keys (`SessionRecord.kt:10`, `DailyAggregate.kt:9`): a
session is identified by `(packageName, start)` with an index on `start`, and a daily total by
`(packageName, day)` with `day` as ISO text so range queries and ordering are plain string
comparisons. Both DAOs upsert with `REPLACE` (`SessionRecordDao.kt:12-13`, `DailyAggregateDao.kt:12-13`);
`totalsFor` compares `day` as text (`DailyAggregateDao.kt:18-19`), which is why the ISO format
matters. Version 1 exports its schema to `app/schemas/` (KSP arg at `app/build.gradle.kts:43-45`);
there is no `fallbackToDestructiveMigration` anywhere, so a version bump without a migration fails
loudly instead of erasing the history this change exists to keep.

Room arrives through KSP: plugin `app/build.gradle.kts:5`, artifacts
`app/build.gradle.kts:62-64`, versions pinned in `gradle/libs.versions.toml:14-15,32-34,46`
(Room 2.8.5, KSP 2.3.12, both proven against AGP 9.4 / Kotlin 2.4.21).

Privacy: `<application android:allowBackup="false">` (`AndroidManifest.xml:31`) keeps the database
out of Auto Backup and out of device-to-device restore.

## Physical traceability

- The data lives in `/data/data/com.foco.app/databases/foco.db` (+ `-wal`) on the device; checked by
  hand on the emulator after the first start: 14 raw session rows and 2 daily totals for
  `2026-10-18`, and a second start left the same counts.
- The source of a row is `UsageRepository.loadDay` (read-only `UsageStatsManager.queryEvents`,
  clamped to the day, package policy applied) — the store adds no filtering of its own.
- `app/src/test/java/com/foco/app/data/HistoryConsolidationTest.kt` — JVM, fake DAOs, no Android:
  stored-and-read-back (`:25`), survives the platform dropping the day (`:40`), twice is identical
  (`:55`), today is skipped (`:68`).
- `app/src/test/java/com/foco/app/data/HistoryRetentionTest.kt` — JVM: prune removes an old raw row
  and keeps its total (`:24`), an inside-the-window row survives (`:41`), an empty day stays pending
  (`:51`).
- `app/src/test/java/com/foco/app/BackupDisabledManifestTest.kt:15` — reads the manifest text.
- `app/src/androidTest/java/com/foco/app/data/db/FocoDatabaseTest.kt` — real SQLite in memory: both
  composite keys and `REPLACE` (`:36`), one total row per app and day (`:50`), the prune statement
  itself (`:63`).

## Limits and failure modes

- The backfill can only see what the platform still serves (~7 days). Install the app, ignore it for
  a week, open it: the gap before that week is gone. `nightly-consolidation` exists for exactly this.
- Today is never stored, on purpose: its sessions are still being built. If the app is never opened
  again tomorrow, today never becomes a stored day — same follow-up change.
- A day whose totals exist is skipped forever, so a partial write that reached the totals would be
  invisible; the raw-first order makes that unreachable in practice, and the retry is idempotent.
- The prune cutoff is computed with `ZoneId.systemDefault()` at consolidation time. Crossing
  time zones shifts it by hours — harmless, it only trims a cache.
- `lifecycleScope` ties the run to the activity: leaving mid-run cancels it, and the next start
  retries the unfinished days. Any other exception is swallowed on purpose (best effort); a
  persistent failure shows as missing history, never as a crash and never as an erased database.
- `allowBackup="false"` also means an uninstall loses the history: there is no restore path by
  design.
- The empty-day branch retries that day on every start (no totals are written). Cheap on a personal
  device; a "day done" marker was not invented for it.

## Touched APIs

- `androidx.room` 2.8.5 (`@Entity`, `@Dao`, `@Query`, `@Insert(REPLACE)`, `Room.databaseBuilder`) via
  KSP 2.3.12, with `room.schemaLocation` → `app/schemas/`.
- `androidx.lifecycle.lifecycleScope` + `kotlinx.coroutines` (`launch`, `Dispatchers.IO`,
  `CancellationException` rethrown so cancellation is not swallowed).
- `java.time` (`LocalDate`, `ZoneId`) for the window and the cutoff; the DAO sees `Long` millis and
  ISO strings only.
- Plate: `<application android:allowBackup>` — no new permission.
- `UsageStatsManager` stays read-only; the store never writes outside its own database.

## Implicit decisions

- **Room + KSP** (human decision in the proposal): the alternative was raw `SQLiteOpenHelper`; the
  exported schema is the migration baseline, committed as `app/schemas/.../1.json`.
- **Raw first, totals second**: recoverability over "totals visible earlier" — a retried day is
  idempotent, a day marked done without raw rows is not recoverable.
- **The DAO row is the read model** (`week(until)` returns `List<DailyAggregate>`): no DTO until a
  screen needs another shape. `week(until)` covers the seven days ending at `until`.
- **Injected DAO + reader, Room factory in the companion**: the logic runs on the JVM with fakes; the
  only Android-touching entry point is `forApp`.
- **No destructive fallback, no `room-testing` yet**: migrations arrive with the first version bump.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. When you open the app once a day, which day does the startup backfill store, and why is today
   left out?
   > El backfill guarda lo que hiciste hoy y los seis días anteriores, descarta lo que hiciste hoy y
   > guarda los demás o sea guarda ayer y 5 días para atrás desde ayer.
   >
   > — the human, 2026-10-19
2. Why can the same day be consolidated twice without duplicated rows — which part of the schema
   provides that, and what breaks if only the totals were written before the raw sessions?
   > No se duplica ya que el upsert de la sesión es replace; la identidad es (packageName, start) y
   > si el proceso muere después de escribir las crudas, el día no tiene totales, queda pendiente y
   > el próximo arranque lo reintenta.
   >
   > — the human, 2026-10-19 (la segunda mitad, tras la ronda 2)
3. What happens to the stored history if you do not open the app for eight days, and which
   follow-up change is supposed to cover it?
   > De lo ya guardado se elimina todos los session record pero los daily aggregate no se borran.
   >
   > — the human, 2026-10-19 (con el ajuste aceptado: solo las crudas fuera de la ventana de siete
   > días; el hueco no consolidado lo cubre `nightly-consolidation`)

When satisfied, the sign-off is recorded in `/spec-verify` (`understood_by` + `understood_at` in
`.openspec.yaml`).
