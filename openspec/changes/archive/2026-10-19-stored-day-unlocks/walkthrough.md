# Walkthrough — stored-day-unlocks (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them. The sign-off itself — the transcribed
> answers plus `understood_by` / `understood_at` — is recorded in `/spec-verify`, never by the agent.

## Mechanism

A finished day's unlock count is no longer a number the app only knows while the platform still
serves the day: the consolidation that already stores the day's sessions and per-app totals now
stores the count too, in a third table. Nothing new triggers it — the same app-start backfill that
existed before writes it.

The path, hop by hop:

1. **The trigger is app start.** `MainActivity.onCreate` calls `consolidateHistory()`
   (`MainActivity.kt:57`), which guards the one-shot with an `AtomicBoolean`
   (`MainActivity.kt:80`, field at `:98`) and launches the work on **IO**
   (`lifecycleScope.launch(Dispatchers.IO)`, `MainActivity.kt:82`). Inside it builds the store over
   the platform reader — `SessionHistoryStore.forApp(applicationContext) { date -> repository.loadDay(date) }`
   (`MainActivity.kt:84`, repository at `:81`) — and calls `.consolidate(LocalDate.now())`
   (`MainActivity.kt:85`). The whole call is wrapped in a catch that rethrows cancellation, logs any
   other failure and swallows it (`MainActivity.kt:86-93`).
2. **Which days are pending.** `SessionHistoryStore.consolidate(today)`
   (`SessionHistoryStore.kt:30-35`) asks the totals table which days exist —
   `aggregates.storedDays()` (`:31`) → `SELECT DISTINCT day FROM daily_aggregate`
   (`DailyAggregateDao.kt:15-16`) — and subtracts them from the finished days of the window
   (`SessionHistoryStore.kt:32`; `finishedDays` is today-1 … today-6, `AvailableDays.kt:18-21`).
   Each pending day goes through `consolidateDay` (`SessionHistoryStore.kt:33`) and afterwards the raw
   rows are pruned (`:34`).
3. **One platform read per day.** `consolidateDay(day)` (`SessionHistoryStore.kt:57-64`) calls
   `readDay(day)` **once** (`:58`) — in the app that is `UsageRepository.loadDay`
   (`UsageRepository.kt:60-105`), which queries the platform (`UsageStatsManager.queryEvents`,
   `UsageRepository.kt:66`), collects the `KEYGUARD_HIDDEN` marks of the requested day into
   `unlockTimes` (`UsageRepository.kt:70-78`, returned at `:103`) and returns sessions plus those
   marks (`DayData.kt:9-12`). No second read is made anywhere: the count is `size` of what this read
   already produced.
4. **The early exit.** If the read returned no sessions, `consolidateDay` returns immediately
   (`SessionHistoryStore.kt:59`). That day gets **no totals and no count** (the comment at `:56` says
   so), and because the pending set comes from the totals table (`:31-32`), the day stays pending and
   is retried on the next start while the platform still serves it.
5. **Three writes, in this order** (`SessionHistoryStore.kt:61-63`):
   1. `sessions.upsert(...)` — the raw rows (`:61`),
   2. `aggregates.upsert(totalsOf(day, data.sessions))` — the per-app totals (`:62`),
   3. `unlocks.upsert(DailyUnlock(day.toString(), data.unlockTimes.size))` — the count (`:63`).
   The day key is `LocalDate.toString()`, i.e. ISO-8601 text, the same shape `daily_aggregate` uses
   (`DailyAggregate.kt:9-13`).
6. **What that write is physically.** `DailyUnlock` is a Room entity named `daily_unlock`, one row
   per day, `day` as primary key (`DailyUnlock.kt:9-12`). `DailyUnlockDao.upsert`
   (`DailyUnlockDao.kt:12-13`) is an `@Insert(onConflict = REPLACE)`, so a re-consolidation of the
   same day overwrites the row instead of failing or duplicating. There is no delete in the DAO
   (`DailyUnlockDao.kt:8`).
7. **Reading it back.** `SessionHistoryStore.unlocksFor(date)` (`SessionHistoryStore.kt:54`) →
   `DailyUnlockDao.countFor(day)` — ``SELECT `count` FROM daily_unlock WHERE day = :day``
   (`DailyUnlockDao.kt:15-16`) — and the return type is `Int?`: a missing row reads as `null`, a
   stored zero reads as `0`.
8. **Who calls it today: nobody in the shipped app.** The only callers of `unlocksFor` are the new
   JVM test (`StoredUnlocksTest.kt:34,44,49,62,74`). Showing the count of a past day belongs to
   `week-screen`, which declares a dependency on this change (`tasks.md:31`).
9. **The upgrade that makes room for the table.** The database leaves version 1:
   `FocoDatabase` declares `version = 2` and registers the entity and the DAO
   (`FocoDatabase.kt:12-16`, `:23`), and the app's builder registers a hand-written migration —
   `.addMigrations(FocoDatabase.MIGRATION_1_2)` (`SessionHistoryStore.kt:89`). `MIGRATION_1_2`
   (`FocoDatabase.kt:27-34`) runs one statement through `SupportSQLiteDatabase.execSQL`
   (`:29-32`):
   `CREATE TABLE IF NOT EXISTS \`daily_unlock\` (\`day\` TEXT NOT NULL, \`count\` INTEGER NOT NULL, PRIMARY KEY(\`day\`))`.
   On an install that already had `foco.db`, that statement runs the first time the database is
   opened and every existing row of `session_record` and `daily_aggregate` is left where it is.

The one-line version: **the app-start backfill reads each finished day once, stores its sessions, its
totals and now its unlock count in `foco.db`, and the count can be read back for one day as `null`
(never stored) or a number (0 is a real answer)**; the version bump exists only to create the table
that holds it.

## Physical traceability

- **Where the state lives**: one SQLite file, `foco.db` (`DATABASE_NAME`, `SessionHistoryStore.kt:76`),
  in the app's private storage (`/data/data/com.foco.app/databases/` on device). It is built once per
  process by `Room.databaseBuilder` behind a `@Volatile` + `synchronized` singleton
  (`SessionHistoryStore.kt:79-90`): created on first use and living as long as the process. There are
  two production entry points to that singleton, `MainActivity.kt:84` (which consolidates) and
  `AppDetailScreen.kt:71` (which only reads); both share the same instance.
- **The three tables of `foco.db`** (`FocoDatabase.kt:12-16`, exported schema under
  `app/schemas/com.foco.app.data.db.FocoDatabase/`):
  - `session_record` — one raw session, key `(packageName, start)`, pruned when it leaves the
    seven-day window (`SessionRecordDao.kt:12-19`). Written and pruned by this change's code path but
    untouched by it as a table.
  - `daily_aggregate` — one per-app, per-day total, key `(packageName, day)`, `day` as ISO text
    (`DailyAggregate.kt:9-13`). The **daily count of rows in this table is what decides whether a day
    is consolidated at all** (`SessionHistoryStore.kt:31-32`), so this table also gates the new write.
  - `daily_unlock` — **new here**. Columns `day` (TEXT, primary key) and `count` (INTEGER, not null)
    (`DailyUnlock.kt:9-12`; exported `createSql` at `2.json:82`). One row per consolidated day, one
    small row per day, forever.
- **Who writes it**: only `SessionHistoryStore.consolidateDay` (`SessionHistoryStore.kt:63`), itself
  only reached from `consolidate` (`:30-35`), itself only reached from `MainActivity.consolidateHistory`
  (`MainActivity.kt:79-95`). There is no other writer in the app (no UI path, no second backfill).
- **Who reads it**: only `SessionHistoryStore.unlocksFor` (`SessionHistoryStore.kt:54`), and in the
  shipped app nobody calls that yet — the tests are the only callers today
  (`StoredUnlocksTest.kt`). `week-screen` is the intended first consumer.
- **Lifecycle / retention**: raw rows are deleted by `sessions.pruneBefore(rawCutoff(today))`
  (`SessionHistoryStore.kt:34` → `SessionRecordDao.kt:18-19`); the count is explicitly **outside**
  that retention — the DAO has no delete statement (`DailyUnlockDao.kt:8,15-16`) and the prune
  touches only `session_record`, so a day's count outlives its raw rows, exactly like its totals
  (`proposal.md:20-21`; the bounded-history contract at
  `openspec/specs/usage-tracking/spec.md:233-235`). Rows are replaced, never accumulated: the
  primary key is the day (`DailyUnlock.kt:9`), so re-consolidating a day keeps one row.
- **No timestamps are stored**: the entity holds the day and the count only (`DailyUnlock.kt:10-12`);
  the unlock *times* the platform read produced are dropped after the `size` is taken
  (`SessionHistoryStore.kt:63`). The rejection of the timestamps shape is recorded in
  `proposal.md:49,56` and in `DECISIONS.md:352`.
- **The upgrade path**: `exportSchema = true` and `version = 2` (`FocoDatabase.kt:14-15`) with
  `room.schemaLocation` pointed at the committed `app/schemas/`
  (`build.gradle.kts:48-51`), so `1.json` and `2.json` are the migration baseline; **no destructive
  fallback** is configured anywhere — the builder at `SessionHistoryStore.kt:85-89` only adds the
  migration, so a missing path would fail loudly instead of wiping the file.
- **Nothing leaves the device**: no network call, no new permission and no manifest change in this
  change; `android:allowBackup="false"` keeps the database out of cloud backup
  (`AndroidManifest.xml:31`, contract at `openspec/specs/usage-tracking/spec.md:245-247`).

## Limits and failure modes

- **A day with no sessions stores nothing at all.** `consolidateDay` returns before any write
  (`SessionHistoryStore.kt:59`), so the day has no totals and no count, and `unlocksFor` answers
  `null` for it. It is not silently ignored: the day stays pending (`:31-32`) and the next start
  retries it, which is why `HistoryRetentionTest.kt:51-64` ("an empty day is retried on the next
  run") still passes.
  - `unclear:` **a day with keyguard unlocks but no usage sessions is not stored either** — the
    early return at `SessionHistoryStore.kt:59` fires on empty *sessions* while the count would come
    from `unlockTimes` (`:63`), so the count is discarded. No spec requirement, task or decision
    mentions this combination; the code is the only witness. The parent should confirm whether that
    is intended.
- **Days consolidated before version 2 keep no count, forever.** The upgrade only creates an empty
  table (`FocoDatabase.kt:29-32`), and nothing ever re-reads an old day: the pending set is computed
  from the totals table (`SessionHistoryStore.kt:31-32`) and an old day already has totals, so it is
  never consolidated again. No retro-fill is deliberate (`proposal.md:59-60`). The device test pins
  the empty table after the upgrade (`UnlockMigrationTest.kt:38`), and a day in that state reads as
  `null` — "no stored count", not zero.
- **Crash between the totals and the count: the count is lost and not repaired.** The order is raw
  rows → totals → count (`SessionHistoryStore.kt:61-63`). Killing the process after `:62` and before
  `:63` leaves that day with sessions and totals and **no count row**; on the next start the day is
  no longer pending, because pending is `finishedDays - storedDays` and `storedDays` comes from the
  totals table (`:31-32`) — so it will never be retried, and `unlocksFor` answers `null` for it
  forever. The comment at `:60` documents the other side of this trade: raw rows go first so that a
  failure *before* the totals leaves the day pending, and the retried writes are idempotent
  (`REPLACE` on all three tables, `SessionRecordDao.kt:12`, `DailyAggregateDao.kt:12`,
  `DailyUnlockDao.kt:12`). The count is last precisely because it has no self-healing.
- **The table is never pruned.** One tiny row per consolidated day, growing with the days the app has
  ever run on, forever; there is no delete in `DailyUnlockDao` (`DailyUnlockDao.kt:8`). This is the
  chosen policy (the count joins the totals side of retention, `proposal.md:20-21`), not an
  oversight, but it is unbounded in time.
- **The count is per day, not per app, and carries no time.** It cannot answer "how many unlocks
  while app X was open" or "at what time did I unlock"; that shape was rejected (`proposal.md:49,56`).
  Reversing it means a new column or a new table plus a new migration.
- **A failed write is a failed day, surfaced only in logcat.** Any exception from the three writes
  propagates out of `consolidate` into the backfill's catch (`MainActivity.kt:88-93`), which logs
  `"History backfill failed"` and swallows it: the app stays usable, and the days the run did not
  reach stay pending for the next start. A count lost to a crash mid-run (above) is not recovered by
  that retry, because its day is no longer pending.
- **Force close / kill**: whatever SQLite committed stays; nothing is held in memory across
  processes. The only in-process state is the `AtomicBoolean` (`MainActivity.kt:80,98`) that keeps
  two backfills from running at once, and the `@Volatile` database singleton
  (`SessionHistoryStore.kt:79-90`).
- **The database or app data deleted** (clear storage, uninstall, or a restore denied by
  `allowBackup="false"`): `foco.db` is gone. There is nothing to migrate and nothing to recover: the
  next start opens a fresh file at version 2 (the migration does not run — there is no version 1 to
  come from) and consolidates only the days the platform still serves, so counts and totals of
  older days are lost for good.
- **Concurrency**: the writer runs on IO from app start (`MainActivity.kt:82`) while the detail
  screen reads the same `foco.db` on its own IO coroutine (`AppDetailScreen.kt:71,80-104`). Room
  serializes the statements; the count is one row keyed by the day with a `REPLACE` upsert, so a
  read that races the write sees either the day's count or no count, never a duplicate or a partial
  row. A second `consolidate` in the same process is refused by the `AtomicBoolean`
  (`MainActivity.kt:80`); a store used from the detail screen never calls it at all.
- **No network at all**: a missing count is a missing row in a local table. There is no server, no
  sync and no remote schema.
- **A migration that drifts from the entity kills the open.** Room validates the migrated file
  against the compiled schema when it opens it; if the hand-written SQL and the entity ever disagree,
  the failure is an exception at open time (`SessionHistoryStore.kt:85-89` builds the file that the
  app will then use), not a silent difference. That is what `MigrationTestHelper.runMigrationsAndValidate`
  checks on device (`UnlockMigrationTest.kt:34`).
- **The migration is only proven on a device.** The JVM suite runs the store over in-memory fakes
  (`HistoryFakes.kt:44-54`, `StoredUnlocksTest.kt:21-25`) which implement the DAO *interfaces* in
  Kotlin: no SQLite, no version 1 file, no `CREATE TABLE`, no Room validation. They prove the store's
  logic (which value is written, when, and what a missing row reads as) and nothing about the
  database. `UnlockMigrationTest` (`UnlockMigrationTest.kt:22-40`) writes real rows into a real
  version-1 file with raw SQL (`:25-31`), runs the migration and validates (`:34`), then asserts the
  sessions and totals survived and the new table is empty (`:36-38`). Those two facts — the upgrade
  keeping data, and the SQL matching the compiled schema — cannot be observed without a real SQLite.

## Touched APIs

- **`androidx.room` entity API** — `@Entity(tableName = "daily_unlock", primaryKeys = ["day"])` on a
  data class (`DailyUnlock.kt:9-12`). It is the same shape `DailyAggregate` already uses
  (`DailyAggregate.kt:9`): a text day key, so one day cannot have two rows.
- **`@Dao` + `@Insert(onConflict = OnConflictStrategy.REPLACE)` + `@Query`** on
  `DailyUnlockDao` (`DailyUnlockDao.kt:9-16`). `REPLACE` is what makes a repeated consolidation an
  overwrite instead of a primary-key failure; the `@Query` returning `Int?` is how "no stored count"
  is told apart from zero.
- **`androidx.room.RoomDatabase` / `@Database`** — `version = 2`, the entity list and the third
  abstract accessor `dailyUnlocks()` (`FocoDatabase.kt:12-16,23`). Bumping the version is what makes
  Room look for a migration instead of rejecting the file it finds.
- **`androidx.room.migration.Migration` + `androidx.sqlite.db.SupportSQLiteDatabase.execSQL`** —
  `MIGRATION_1_2` (`FocoDatabase.kt:27-34`). `migrate(db: SupportSQLiteDatabase)` is the only door
  Room gives for raw DDL during an upgrade, and `execSQL` executes the `CREATE TABLE` that Room will
  then validate against the exported schema (`2.json:82`).
- **`RoomDatabase.Builder.addMigrations`** — `SessionHistoryStore.kt:89`. Without registering it, a
  version-1 file would have no path to version 2 and Room would fail at open; it is also the reason
  no destructive fallback is configured.
- **`androidx.room:room-testing` → `androidx.room.testing.MigrationTestHelper`** (test-only): the
  constructor with the instrumentation and the database class (`UnlockMigrationTest.kt:17-20`),
  `createDatabase(name, 1)` to write a real version-1 file (`:24`), and
  `runMigrationsAndValidate(name, 2, true, MIGRATION_1_2)` (`:34`) which migrates that file and
  compares the resulting tables against the compiled schema. Declared at
  `libs.versions.toml:36` and wired as `androidTestImplementation(libs.androidx.room.testing)`
  (`app/build.gradle.kts:80`), pinned to the same `room` version alias as the runtime
  (`libs.versions.toml:14,36`), so it is never shipped in the app.
- **SQLite reads in the test through `SupportSQLiteDatabase`** — `query("SELECT * FROM $table").use { it.count }`
  (`UnlockMigrationTest.kt:42-43`) and raw `INSERT` statements into the version-1 file (`:25-31`):
  the test speaks SQL directly because it is examining the file, not the app's code.
- **Gradle androidTest assets source set** — `getByName("androidTest").assets.srcDir("$projectDir/schemas")`
  (`app/build.gradle.kts:42-45`). `MigrationTestHelper` reads the exported schema JSON files as
  *assets* at runtime, and they live in `app/schemas/`, not in the test source set; this line is what
  puts them on the device.
- **`kotlinx-serialization-bom` 1.8.1 → `platform(...)`** — `app/build.gradle.kts:55-57`, alias at
  `libs.versions.toml:16,37`. Room 2.8 ships its schema bundles (`androidx.room.migration.bundle`)
  compiled against kotlinx.serialization 1.8, while the app's classpath resolved 1.7.3 through
  `androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.9.4`; AGP's *consistent resolution*
  copies that version into the androidTest classpath, so `MigrationTestHelper` died with
  `AbstractMethodError: typeParametersSerializers()` before reading a single schema. Aligning the
  runtime to 1.8.1 is what lets the device test run at all; the app itself calls no serialization API.
- No new runtime dependency beyond that version alignment, no new permission, no manifest change, no
  network, no new platform API.

## Implicit decisions

- **The count is a trailing field of the existing consolidation, not a new read or a new job**
  (`SessionHistoryStore.kt:63`, task 2.1 in `tasks.md:12`): the same `readDay(day)` call that already
  produced the sessions and totals (`:58`) yields `unlockTimes.size`, so a finished day costs exactly
  the two platform reads it cost before this change (none extra) and no new permission.
- **The write order is deliberate and asymmetric** (`SessionHistoryStore.kt:61-63`): raw rows, then
  totals, then the count. The comment at `:60` explains the raw-first half (a failure before the
  totals leaves the day pending so the next start retries it); the count goes last because it is the
  one write with no retry behind it, and doing it first would create the inverse corruption — a
  count for a day with no totals, which would also mark the day as consolidated the moment the totals
  table is inspected. The honest cost is the lost-count window described in the limits.
- **`REPLACE` instead of guarding with a check**: the DAO overwrites on the day key
  (`DailyUnlockDao.kt:12`), so "consolidate twice" stays idempotent without the store asking first.
  Pinned by `StoredUnlocksTest.kt:53-63`("consolidating the same day again keeps one count": one row,
  the same number).
- **The read is nullable on purpose** (`DailyUnlockDao.kt:16`, `SessionHistoryStore.kt:53-54`): the
  signature `unlocksFor(date): Int?` carries the whole distinction between "this day was never
  consolidated" and "this day had no unlocks". Zero is stored and returned as zero
  (`StoredUnlocksTest.kt:38-45`).
- **The day key is the same ISO text the totals table uses** (`LocalDate.toString()` at
  `SessionHistoryStore.kt:54,63`, matching `DailyAggregate.kt:11`): no type converter, the day sorts
  as text and can be compared with the totals table's `day` column directly.
- **Retention by omission**: the DAO simply has no delete statement (`DailyUnlockDao.kt:8`), so the
  prune cannot reach it even if someone later widens the prune query — the count is on the totals
  side of the retention policy (`proposal.md:20-21`), not the raw side.
- **A hand-written migration whose SQL text mirrors the exported schema** (`FocoDatabase.kt:29-32`
  vs `2.json:82`). Room's `CREATE TABLE` text in `2.json` is what the runtime validates the migrated
  table against (and what `runMigrationsAndValidate` compares after running the migration), so the
  migration's statement has to produce a table with the same columns, affinities, nullability and
  primary key — `day` TEXT NOT NULL, `count` INTEGER NOT NULL, `PRIMARY KEY(day)`. The doubled
  backticks in the Kotlin string exist only so the SQL text has the backticks SQLite expects.
- **`exportSchema` stays on and the baseline is committed** (`FocoDatabase.kt:15`,
  `app/build.gradle.kts:48-51`, `app/schemas/com.foco.app.data.db.FocoDatabase/1.json` and `2.json`):
  a new baseline is produced by the build (not hand-written) and committed next to version 1, which
  is what makes the device migration test possible at all.
- **No destructive fallback anywhere** (`SessionHistoryStore.kt:85-89` adds only the migration): the
  policy the stored-history foundation recorded is kept — a schema problem fails loudly rather than
  deleting the user's history.
- **The store's constructor gained a third DAO parameter** (`SessionHistoryStore.kt:22-27`, `unlocks`
  at `:25`), which is why every construction site had to change: `forApp` (`:91-96`) and the tests
  `StoredUnlocksTest.kt:24-25`, `HistoryConsolidationTest.kt:19`, `HistoryRetentionTest.kt:21`,
  `HistoryWeekTest.kt:14-19`. The three older tests pass a throwaway `FakeDailyUnlockDao()` because
  they assert sessions and totals, not counts — a compile-level change, not a behavior change, which
  is why they assert exactly what they asserted before.
- **A fourth in-memory fake rather than mocking** (`HistoryFakes.kt:44-54`, following the two that
  already existed at `:10-42`): the store still runs on the JVM, which is what keeps the JVM suite
  (`gradlew.bat test`) the fast axis and the device test reserved for what only SQLite can prove.
- **The dependency is test-only and version-pinned to the Room alias** (`libs.versions.toml:36`,
  `app/build.gradle.kts:80`): no version literal appears twice, and nothing of `room-testing` reaches
  the release APK.
- **The serialization runtime was raised from 1.7.3 to 1.8.1 so the migration test can run**
  (`app/build.gradle.kts:55-57`): the alternative was to drop `room-testing` and hand-build a
  version-1 SQLite file inside the test. The alignment won because Room 2.8 already ships code that
  needs 1.8 and 1.7.3 arrived only as an accident of the lifecycle graph; the cost is a small,
  backward-compatible bump in the shipped APK, with no serialization API the app calls. Device proof:
  `UnlockMigrationTest` -> `OK (1 test)` on API 36.
- **The new count is not shown anywhere yet** (`SessionHistoryStore.kt:54` has no production caller):
  this change stores and exposes; the dated day view is `week-screen`'s job, which depends on this
  change (`tasks.md:31`) — one idea per change, as decided (`DECISIONS.md:352`).
- **The `FocoDatabaseTest` device test was not extended** (`app/src/androidTest/.../FocoDatabaseTest.kt:35-70`
  still covers `session_record` and `daily_aggregate` only): the new table's SQL-level behavior is
  covered where it matters here, in the migration test; nothing was added just for symmetry.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. A day that version 1 had already consolidated now reads as "no stored count" forever. Which table
   holds the count, which function is the one that writes counts for other days, and what exactly
   prevents that old day from ever being given one?
   > **Answer (Nschkurko, 2026-10-19):** La tabla `daily_unlock` guarda el count; la que lo escribe es
   > `SessionHistoryStore.consolidateDay(day)`. Lo que impide que el día viejo reciba un count es que
   > `consolidateDay` nunca corre para ese día viejo, por ende el count termina siendo `null`: el día
   > viejo ya tiene filas de totales, así que nunca llega a `consolidateDay`.

2. The process is killed during a start, right after a day's totals are stored but before its count.
   Describe that day's rows afterwards, and state whether the next start repairs them — naming the
   function and the table that decide it.
   > **Answer (Nschkurko, 2026-10-19):** `consolidateDay` son escrituras separadas de Room, así que si
   > se muere el proceso `daily_unlock` se queda con cero filas; al reiniciar, `consolidate` no repara
   > nada. La decisión la toman la función `SessionHistoryStore.consolidate` y la tabla
   > `daily_aggregate`.

3. Next week you want to persist one more number per day in the same database. Which files must
   change, and what must the new migration's SQL be equal to for
   `MigrationTestHelper.runMigrationsAndValidate` in `UnlockMigrationTest` to accept it?
   > **Answer (Nschkurko, 2026-10-19):** (a) Podemos agregar una columna nueva en la tabla
   > `daily_unlock` o agregar otra tabla. Deberíamos cambiar sí o sí `DailyUnlock`, `FocoDatabase`, el
   > schema de `FocoDatabase`, `SessionHistoryStore` y los archivos para testing; si es una nueva
   > tabla, un DAO y un archivo `Daily...`. (b) El SQL no tiene que ser textualmente igual a nada, pero
   > debe producir una tabla cuyo `TableInfo` sea igual al que Room deriva de
   > `app/schemas/com.foco.app.data.db.FocoDatabase/3.json`.
