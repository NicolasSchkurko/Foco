# Tasks — consolidation-raw-reset

- [x] 1. Extend `HistoryConsolidationTest`: a failed marker followed by a changed read must leave only
      the new raw rows.
- [x] 2. Add a DAO operation that deletes the raw rows of one day.
- [x] 3. Call it at the start of `consolidateDay`, before the raw upsert; keep totals and marker in the
      existing order.
- [x] 4. Re-run `HistoryConsolidationTest`, retention/week tests and the full JVM suite.
- [x] 5. Run `openspec validate consolidation-raw-reset --strict` and the gate.
