# Walkthrough — prune-stored-history (tech brief)

## Mechanism

One rule, one place, both triggers.

**The cutoff.** `SessionHistoryStore.consolidate(today)` (`SessionHistoryStore.kt:31-40`) already
walked the finished days the platform still serves and pruned the raw rows at the end. Now it
computes one day — `pruneCutoffDay(today)` = `today.minusDays(KEEP_DAYS - 1)` with `KEEP_DAYS = 8L`
(`:77, :86`) — and prunes all three tables against it, in this order (`:37-40`):

1. `sessions.pruneBefore(rawCutoff(cutoffDay))` — `DELETE FROM session_record WHERE end < :cutoff`
   (`SessionRecordDao.kt:18`), cutoff in epoch millis at the start of the cutoff day on
   `ZoneId.systemDefault()`, the same calendar the reader uses (`:80-81`).
2. `aggregates.deleteBefore(cutoffDay.toString())` — `DELETE FROM daily_aggregate WHERE day < :cutoffDay`
   (`DailyAggregateDao.kt:26`), ISO text comparison.
3. `consolidatedDays.deleteBefore(cutoffDay.toString())` — the same, on the marker
   (`ConsolidatedDayDao.kt:25`), which takes its unlock count with it.

**The boundary.** Today and the seven days before it stay; today minus eight and older go. That is
one day more than the seven-day read window on purpose (see *Implicit decisions*).

**Two numbers, not one.** `SIX_DAYS = 6L` (`:85`) keeps driving `week()` (`:43-44`, the seven-day
read window: `today.minusDays(SIX_DAYS)` through `today`). Pruning uses `KEEP_DAYS = 8L` (`:86`).
They are deliberately unrelated constants: changing the UI window must not silently move the delete
line, and moving the delete line must not change what the ring shows.

**Store first, prune last.** The deletes run after the pending-days loop (`:33-35`). A throw while
reading a day leaves the prune unreachable, so a failed run deletes nothing — the property the
nightly job already relied on, now covering all three tables (`ConsolidationRetryTest.kt`).

**Why every run, not only the worker.** `consolidate` is reached through
`ConsolidationRunner` from both triggers — app start (`MainActivity.kt:81`) and the nightly
`ConsolidationWorker` — so the policy lives in one place and the app-start run cleans up after a
week of Doze, a battery saver or a force-stop (ADR in `DECISIONS.md`).

No schema change: `FocoDatabase` stays at version 3 (`FocoDatabase.kt:14`), no migration, no new
table. The two new DAO methods are queries against columns that already exist.

## Physical traceability

- **File**: `foco.db` in private storage (`SessionHistoryStore.kt:85`), opened once per process by
  `forApp` with the same `@Volatile` + `synchronized` double check and the two migrations
  (`:92-109`). Pruning deletes rows in place; the file does not shrink on disk (SQLite reuses the
  pages) but the retained rows stop growing.
- **Tables touched**: `session_record` (raw sessions, PK `packageName` + `start`), `daily_aggregate`
  (per-app, per-day totals, PK `packageName` + `day`) and `consolidated_day` (one marker per
  finished day, PK `day`, `count` nullable).
- **Keys**: the two day-keyed tables store `LocalDate.toString()` — ISO text that sorts and compares
  as text, the same order `totalsFor` already relies on (`DailyAggregateDao.kt:22`). Raw rows are
  keyed and cut in epoch millis instead, because a session has an instant, not a day.
- **Writers**: `consolidateDay` (`SessionHistoryStore.kt:62-69`) writes; `consolidate` deletes. No
  UI or ViewModel path touches the DAOs directly.
- **Readers after pruning**: unchanged. `week()` reads the seven-day window, `sessionsFor`/
  `unlocksFor` read a single day, `storedDaysFlow` streams the markers. Every reader only asks for
  days inside the read window, so a pruned day is never requested.
- **Lifecycle**: rows enter on consolidation and leave on the first run after their day falls
  outside the eight kept days. A user force-stop just delays that run; the next app start or worker
  does it.

## Limits and failure modes

- **Today minus eight is the first day deleted.** Off by one in either direction is visible: `-7`
  would delete the oldest day the ring still shows, `-9` would let a second guard day accumulate.
  `HistoryRetentionTest` pins both sides (`today.minusDays(8)` gone, `today.minusDays(7)` kept, in
  all three tables).
- **A session that crosses midnight.** A row is filed under its `start` day but pruned by `end`. A
  session starting at 23:50 and ending at 00:10 survives the run whose cutoff is that next midnight,
  because `end < cutoff` is false at equality; it disappears on the following run. Its day's totals
  and marker are already gone, so nothing visible depends on it.
- **A clock or timezone change.** The cutoff is built with `LocalDate.now()` and
  `ZoneId.systemDefault()`, the same pair the reader uses. A device moved back a day prunes less;
  moved forward, it prunes one extra day early — accepted, because the alternative (a UTC day) would
  disagree with every other screen.
- **A run that fails midway.** A throw before the deletes returns `Failed` through
  `ConsolidationRunner.kt:33-34` and nothing is pruned. A process kill *between* the three deletes is
  not a transaction: it can leave an old day with its raw rows gone and its totals or marker still
  there. The next run finishes the job; no reader ever asks for that day.
- **An app closed longer than the guard.** The guard day only helps a *late* run: a marker written
  yesterday for `today-7` survives if today's run happens before the cutoff advances. If the app is
  closed for ten days, the days in between are lost regardless — the platform no longer serves them
  and the store never had them. Pruning makes that loss explicit, it does not cause it.
- **Deletion is final.** Unlike the platform's window, there is no second copy: once a day leaves the
  eight, neither the platform (which serves fewer days) nor the app can rebuild it. That is the
  accepted cost of the reversal recorded in `DECISIONS.md`.
- **Two triggers, one prune.** The single-flight runner already serializes the runs; the deletes are
  idempotent, so a second run deletes nothing.

## Touched APIs

- **Room `@Query` with a `DELETE` and an `Int` return** on `DailyAggregateDao` and
  `ConsolidatedDayDao`: returns the number of rows deleted, mirroring `pruneBefore`. The store ignores
  the value; the fakes use it to keep their write counters honest.
- **`java.time.LocalDate` / `ZoneId.systemDefault()`**: the cutoff day and its start-of-day millis.
  Pure JVM, so the tests run without Android.
- **No new dependency, no WorkManager API, no permission.**
- **Test seams**: `HistoryFakes.kt` implements `deleteBefore` on the two in-memory DAOs
  (`removeIf { it.day < cutoffDay }` / `removeIf { it < cutoffDay }`).

## Implicit decisions

- **Eight kept days, seven visible.** The extra day is a guard: a consolidation that runs late (Doze,
  a force-stop, a phone off overnight) still finds yesterday's `today-7` marker before deleting it,
  so the seven-day window can be rebuilt. The selector does not grow: `availableDays` caps at
  `windowDays = 7` (`AvailableDays.kt:10-13`), so the guard day is never offered to the UI.
- **Read cutoff and prune cutoff are different constants.** `SIX_DAYS` (`:85`) and `KEEP_DAYS` (`:86`)
  express two different intentions that happen to differ by one; sharing a number would make a UI
  change a data-loss change.
- **Milliseconds for raw rows, text days for totals and markers.** `session_record` answers
  "which sessions happened between these instants", so its cutoff must be an instant; the other two
  answer "which day", so they compare ISO text, the format their keys already use.
- **The prune ignores how many rows it deleted.** No log, no UI signal: the change is about a bound,
  not about reporting. `pruneBefore` already worked that way.
- **`consolidated_day` is deleted as a whole row**, count included, rather than nulling the count:
  a marker with no count would be indistinguishable from a day the store never consolidated.
- **The deletes are not wrapped in a transaction.** The three statements touch disjoint tables and
  the reader never asks for a day outside the window; a partial prune is invisible and self-heals on
  the next run. Adding `withTransaction` would be a bigger change than the bound it protects.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. The app is opened on the 15th after last running on the 6th. Say which days that run can store,
   which rows it deletes, and why the guard day does not rescue the days in between.   > **Answer (Nschkurko, 2026-10-20):** El 15 la corrida solo mira los días terminados de la ventana
   > de seis: del 9 al 14 (today-1 a today-6), y puede guardar esos si la plataforma todavía los sirve; el
   > 6, 7 y 8 no los toca. Después poda con cutoff = 15-7 = 8: borra raw con end antes de las 00:00 del 8,
   > y totales/marcadores con day < 8, o sea todo lo viejo. El día de guarda (el 8) no rescata los días de
   > en medio porque no es una ventana de búsqueda: solo evita que se borre un día que ya estaba guardado,
   > y acá el 6, 7 y 8 nunca se guardaron (el 6 la app guardó hasta el 5); la plataforma quizá los tenga,
   > pero finishedDays no los pide, no hay backfill de días viejos.
2. A session starts at 23:50 on day X and ends at 00:10 on the next day. Explain which day's row it
   is, which cutoff keeps it alive at first, and why the day's totals and count are gone before that
   row is.   > **Answer (Nschkurko, 2026-10-20):** La fila es del día X, porque la sesión se guarda por su start y
   > sessionsFor(X) la levanta, con el end clampado a las 00:00 de X+1. Se mantiene viva mientras el cutoff
   > crudo (00:00 del día today-7) no pase ese instante: en la corrida con cutoff = 00:00 de X+1, end <
   > cutoff da false porque son iguales, así que sobrevive; muere en la siguiente, con cutoff = 00:00 de
   > X+2. Los totales y el marcador del día X se borran una corrida antes, cuando cutoffDay = X+1, porque
   > se comparan por día: "X" < "X+1" es true, mientras que la fila cruda se compara por instante y su end
   > es exactamente el cutoff, no menor; queda huérfana una corrida.
3. The consolidation throws while reading the third pending day. State what was stored, what was
   pruned, and what the next run does — and say why moving the deletes to the beginning of
   `consolidate` would be a bug even though the rows are old.   > **Answer (Nschkurko, 2026-10-20):** Si tira excepción leyendo el tercer día pendiente, los dos
   > primeros quedaron guardados completos (raw, totales y marcador), el tercero no escribió nada y los
   > demás ni se miraron; no se podó nada porque los DELETE están después del loop
   > (SessionHistoryStore.kt:33-40) y la excepción los saltea. La corrida siguiente recalcula pendientes
   > desde consolidated_day: los dos primeros tienen marcador y se saltean, el tercero sigue pendiente y
   > se reintenta, y recién si termina bien poda. Mover los DELETE al principio sería un bug porque un run
   > que después falla ya habría borrado filas viejas que no tienen segunda copia (la plataforma ya no las
   > sirve): una falla pasaría de costar un reintento a perder días para siempre; la garantía es "guardo
   > todo, después podo".
