# Walkthrough — consolidation-raw-reset

## What this change does

A day is consolidated in three steps: its raw sessions, its per-app totals, then its completion
marker. If the marker write fails, the day stays pending and the next run reads it again. Before
this change the second run only upserted the new raw rows, so any row from the first read whose
`start` had changed survived. The detail merged both sets and the export read both, while the totals
came from the last read. Now the day's raw rows are deleted before the new rows are written, so a
retry replaces the day instead of adding a second version of it.

## Mechanism

- `SessionRecordDao.deleteBetween(from, to)` deletes every raw row whose `start` is in the
  half-open day range (`SessionRecordDao.kt:15-16`).
- `SessionHistoryStore.consolidateDay` calls it immediately before the raw upsert
  (`SessionHistoryStore.kt:62-71`). The order stays raw → totals → marker.
- `rawStart(day)` is the one calendar-to-millis conversion used by the delete, `sessionsFor` and the
  prune cutoff (`SessionHistoryStore.kt:52-53`, `:82-86`), so the delete and the read agree on the
  day boundary.
- The fake DAO mirrors the SQL predicate (`HistoryFakes.kt:20-24`).
- The regression seeds a first read, fails the marker, changes the read's start, re-runs, and
  asserts one row with the new start (`HistoryConsolidationTest.kt:94-113`).

## Physical traceability

| Piece | Where | What it proves |
| --- | --- | --- |
| Delete query | `SessionRecordDao.kt:15-16` | The day range is removed before the new rows |
| Call and order | `SessionHistoryStore.kt:67-70` | Delete, raw upsert, totals, marker |
| Shared day boundary | `SessionHistoryStore.kt:52-53`, `:82-86` | Read and delete use the same `start` range |
| Fake DAO | `HistoryFakes.kt:20-24` | JVM test uses the same predicate |
| Regression | `HistoryConsolidationTest.kt:94-113` | Old start cannot survive a changed retry |

## Limits and failure modes

- **The delete is not transactionally paired with the upsert.** A process death between them leaves
  the day pending with no raw rows until the next run. The marker is still absent, so the next run
  rebuilds the day; this is the same pending-day recovery as before.
- **A changed package set can leave old totals.** The raw rows are replaced, but the totals upsert is
  keyed by `packageName + day`; an app that disappeared from the new read keeps its old totals row.
  The detail and export read raw rows, so they show the new version; the week totals can still count
  the old package. The change's scope is the raw rows; a totals reset would be a separate change.
- **No schema or migration.** Same `session_record` table, range and retention.
- **A day with no sessions is still skipped** before the delete, so an empty read never erases a
  previous day.
- **The delete costs one query per consolidated day.** It runs only for pending days, not for the
  whole retained window.

## Touched APIs and resources

Room DAO (`SessionRecordDao`), the `foco.db` `session_record` table, and the JVM fake. No new
dependency, table, column, backup or permission.

## Implicit decisions

- Delete by the same `start` range used by `sessionsFor`, not by `end` or a text date.
- Keep the existing marker-last order; the marker remains the completion signal.
- Do not delete a day that reads empty; the old rows stay until the platform serves a non-empty read.
- Do not touch totals in this change; the raw rows are the contract's subject.

## Verification

- `gradlew.bat test` green (full JVM suite).
- `HistoryConsolidationTest` green, including the changed-read retry.
- `openspec validate consolidation-raw-reset --strict` valid.
- Gate `--run-tests` green.

## Cognitive sign-off

Answer these in your own words; the sign-off is recorded only after your explicit confirmation.

1. Why must the delete run before the raw upsert, and what still marks the day as complete?
   > El upsert de Room es un REPLACE por clave (packageName, start). Si la segunda lectura trae un start distinto para la misma sesión, la fila vieja tiene otra clave y sobrevive: el detalle y el export terminan mezclando las dos versiones, mientras los totales salen solo de la última. Borrar el rango del día antes del upsert hace que el reintento reemplace el día entero, no que lo parche (SessionHistoryStore.kt:67-68). Lo que sigue marcando el día como completo es ConsolidatedDay, escrito al final (:70); ni las crudas ni los totales son el marcador.
   >
   > — the human, 2026-10-20

2. If the process dies after the delete but before the new raw rows are written, what does the next
   start do, and why is the day not lost?
   > El borrado deja el día sin crudas pero sin marker, así que sigue pendiente. En el siguiente arranque, consolidate(today) vuelve a detectarlo como día sin marcar dentro de la ventana (SessionHistoryStore.kt:32-35) y lo reescribe completo: crudas → totales → marker. No se pierde porque las crudas son una caché reconstruible y la fuente (UsageStats) todavía sirve ese día dentro de los 8 días que se conservan; la ausencia del marker es justamente la señal de reintento (:65).
   >
   > — the human, 2026-10-20

3. Which rows does this change not clean up when the new read has a different package set, and why
   is that outside this change?
   > No limpia las filas viejas de daily_aggregate de paquetes que ya no aparecen en la lectura nueva: aggregates.upsert solo hace REPLACE de las claves (packageName, day) presentes (:69), y no hay un delete de totales por día. Las crudas sí quedan limpias porque el delete es por rango de start. Queda fuera de alcance porque este cambio ataca las filas crudas (session_record) y el merge entre lecturas; resetear los totales del día toca la semántica de los totales semanales y merece su propio cambio.
   >
   > — the human, 2026-10-20
