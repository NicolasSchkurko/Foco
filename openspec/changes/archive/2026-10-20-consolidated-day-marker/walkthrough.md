# Walkthrough — consolidated-day-marker (tech brief)

## Mechanism

The store no longer infers "this day is done" from the totals table. One row per finished day says it,
and that row carries the day's unlock count.

**The row.** `ConsolidatedDay` (`ConsolidatedDay.kt:9-12`) is the entity `consolidated_day`, keyed by
the ISO `day` text, with a **nullable** `count`. A row with a count is a finished day whose count is
known; a row with `null` is a finished day whose count was never stored (the days consolidated before
version 3).

**The pending set.** `SessionHistoryStore.consolidate` (`SessionHistoryStore.kt:32-39`) computes
`finishedDays(today) - consolidatedDays.storedDays()`: the days that still need work are exactly the
days **without a marker**. `storedDaysFlow()` (`:47-48`), which bounds the detail's selector, reads the
same table.

**The write order.** `consolidateDay` (`:62-68`) writes the raw sessions, then the per-app totals, then
the marker **last** (`:68`, `ConsolidatedDay(day, data.unlockTimes.size)`). The marker is the write that
ends the day, so a process death before it leaves the day pending and the next run rewrites it; a day
with a marker was written whole, count included. A day with no sessions still writes nothing and stays
pending (`:63`).

**The read.** `unlocksFor(date)` (`:59`) is `consolidatedDays.countFor(day)`: `null` means no marker or
a marker without a count; `0` is a real answer.

**The upgrade.** `FocoDatabase` moves to version 3 (`FocoDatabase.kt:14`) and registers
`MIGRATION_2_3` (`:40-55`): it creates `consolidated_day`, copies every `(day, count)` from
`daily_unlock`, then records every day that only had totals as `(day, NULL)`, and drops
`daily_unlock`. No destructive fallback.

## Physical traceability

- **Where the marker lives**: `foco.db`, table `consolidated_day`, one small row per finished day.
- **Who writes it**: only `SessionHistoryStore.consolidateDay`, reached from `consolidate`, reached from
  the shared runner (app start and the nightly job).
- **Who reads it**: `storedDaysFlow` (the home ring and the detail's window) and `unlocksFor`
  (the dated dashboard).
- **What the migration costs**: one `CREATE TABLE`, two `INSERT ... SELECT` and one `DROP TABLE`, all
  inside the upgrade; existing sessions and totals are untouched.
- **Nothing leaves the device**: no network, no new permission, no new dependency.

## Limits and failure modes

- **A day consolidated before version 3 keeps an unknown count** (`NULL`), not a fabricated zero; the
  UI still reads it as "no stored count".
- **A failed run leaves the day without a marker**, so it stays pending and the next run retries it —
  that is the difference from the previous schema, where the count could be lost for good.
- **A crash after the totals but before the marker** loses nothing that matters: the next run rewrites
  all three writes (they are `REPLACE`).
- **The marker is pruned with the rest of the old history** (the `prune-stored-history` change owns that
  policy): a day older than the kept window loses its row, its totals and its raw sessions together.

## Touched APIs

- **Room `@Entity` with a composite key and a nullable column** (`ConsolidatedDay.kt:9-12`).
- **Room `@Dao`** with `@Insert(onConflict = REPLACE)`, two `@Query` reads and a `DELETE` by cutoff
  (`ConsolidatedDayDao.kt:12-26`).
- **`androidx.room.migration.Migration` + `execSQL`** for the 2 → 3 upgrade (`FocoDatabase.kt:40-55`).
- **`LocalDate`/`toString()`** as the day key, the shape the other two tables already use.

## Implicit decisions

- **The count moved into the marker instead of living in its own table**: completion and the count are
  the same write, so the count cannot be lost behind a day already marked done.
- **`count` is nullable**: the pre-version-3 days are honestly "finished, count unknown".
- **The marker is written last and nothing else marks completion**: the order is the guarantee; there
  is no second place to look.
- **`daily_unlock` is dropped in the same migration**: one table fewer, one meaning.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. A day from before version 3 opens the database for the first time after the upgrade. Say what row it
   gets, what `unlocksFor` answers for it, and why the migration does not invent a zero.
   > **Answer (Nschkurko, 2026-10-19):** La migración crea `consolidated_day` y le da una fila a cada día
   > previo: si el día tenía fila en `daily_unlock`, copia ese count; si solo tenía totales, inserta
   > `(day, NULL)`; el orden es counts primero y luego los días solo-totales con `INSERT OR IGNORE`, así un
   > count conocido no se pisa con NULL. `unlocksFor(day)` consulta `consolidated_day.countFor` y devuelve
   > ese count o `null` si el marcador tiene `count = NULL` (o no hay fila), y la migración no inventa un
   > cero porque `0` es una respuesta real —día terminado sin unlocks— y usarlo para "no sé" mezclaría dos
   > cosas distintas; `NULL` dice "terminado, count desconocido".
2. The process dies after a day's totals are stored and before its marker. Describe the day's rows, what
   the next start does with that day, and what the old `daily_unlock` schema would have done instead.
   > **Answer (Nschkurko, 2026-10-19):** Si el proceso muere después de escribir los totales y antes del
   > marcador, las sesiones crudas y los totales del día ya están guardados pero no existe fila en
   > `consolidated_day`, así que el día quedó a medias; en el próximo arranque `consolidate` ve que no
   > tiene marcador, lo considera pendiente, lo vuelve a leer de la plataforma y reescribe raw → totales →
   > marcador al final (`SessionHistoryStore.kt:66-68`), sin duplicar porque los upsert son `REPLACE` sobre
   > las mismas claves. Con el esquema viejo de `daily_unlock`, en cambio, la completitud se infería de los
   > totales: si moría entre totales y unlock, el día ya figuraba como guardado, no se reintentaba y el
   > count quedaba perdido para siempre.
3. `consolidate` computes the pending days. Name the table it reads, and say what happens to a day that
   is missing only its marker but already has sessions and totals.
   > **Answer (Nschkurko, 2026-10-19):** `consolidate` lee `consolidated_day`
   > (`consolidatedDays.storedDays()`, `SessionHistoryStore.kt:33`), no los totales, así que un día que
   > tiene sesiones y totales pero le falta el marcador cuenta como pendiente y la próxima corrida lo
   > vuelve a leer y reescribe las tres cosas —raw, totales y marcador al final— sobre las mismas claves
   > `REPLACE`, de modo que se auto-repara sin duplicar filas y termina con su count guardado.
