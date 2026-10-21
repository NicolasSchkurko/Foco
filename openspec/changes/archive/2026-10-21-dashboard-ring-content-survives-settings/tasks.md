# Tasks — dashboard-ring-content-survives-settings

- [x] 1. Hoist the `ringContent` holder (`DashboardScreen.kt:161`) so it survives recreation and
      navigation (remembered above the `NavHost`, or a saveable-backed structure keyed by the
      position key), keeping one owner for position + content.
- [x] 2. Keep the cold-start behavior: a position never shown still composes `RingLoading()` — the
      placeholder's only allowed home (`usage-tracking:491-494`).
- [x] 3. Regression: `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt` — open the
      home, enter Settings, come back → the same day/week content renders with **no** placeholder
      and no reload flash (red today).
- [x] 4. Keep `RingMotionTest` green (no transition/loading regression during moves).
- [x] 5. `gradlew.bat test` green; `node scripts/gates/gate-check.mjs --run-tests` records evidence.
- [x] 6. Tech brief: complete `walkthrough.md` before `--pre-archive`; comprehension sign-off is
      the human's.

- [x] 7. Spec delta: `specs/usage-tracking/spec.md` — ADDED *A restored position renders its content
      without a placeholder*; `openspec validate dashboard-ring-content-survives-settings --strict`
      passes.

## Riskiest slice first

Task 1's ownership change (where the map lives); task 3 pins the visible outcome.

## Deps

`dashboard-unlock-count-not-stored` (chain: `dashboard-periodic-refresh-resilience` → this →
`week-row-opens-a-day-detail`); all three share `DashboardScreen.kt`.
