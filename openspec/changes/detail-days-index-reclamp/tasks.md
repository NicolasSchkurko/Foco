# Tasks — detail-days-index-reclamp

- [ ] 1. Extract a pure helper in `app/src/main/java/com/foco/app/domain/` (beside `availableDays`)
      that resolves a raw index against a days list: inside bounds → same index; beyond → nearest
      valid (clamped); empty list → safe default.
- [ ] 2. `AppDetailScreen.kt`: apply the helper whenever `days` is rebuilt (`:91-104` path), so
      `days[selectedIndex]` at `:120` is always in bounds after a rollover or store change.
- [ ] 3. Regression: `app/src/test/java/com/foco/app/domain/DaySelectionTest.kt` — given a selection
      at the last position and a window that shrinks by one day, the resolved index stays within
      `days` (the case that throws today); also pin the no-shrink no-op.
- [ ] 4. `gradlew.bat test` green; `node scripts/gates/gate-check.mjs --run-tests` records evidence.

- [ ] 5. Spec delta: `specs/usage-tracking/spec.md` — MODIFIED *Day selection within the stored
      history* (+ the shrink-under-selection scenario); `openspec validate detail-days-index-reclamp
      --strict` passes.

## Riskiest slice first

Task 1+2 is the crash fix; the helper keeps task 3 honest without an instrumented rollover.

## Deps

None. The follow-up detail changes (`detail-selection-survives-recreation`,
`detail-refresh-on-resume`) declare a dep on this one because they share `AppDetailScreen.kt`.
