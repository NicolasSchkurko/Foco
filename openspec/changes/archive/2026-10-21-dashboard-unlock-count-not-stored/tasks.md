# Tasks — dashboard-unlock-count-not-stored

- [x] 1. `DashboardScreen.kt:220`: stop coercing `unlocksFor(date)` — keep the nullable result in
      the ring content (or carry an explicit "not stored" state) instead of `?: 0`.
- [x] 2. `:323-327` render path: when the count is not stored, show the unlock line as absent
      (no `0` claim); a stored `0` keeps rendering `0 desbloqueos` — the distinction the store
      already answers.
- [x] 3. Regression (JVM): `app/src/test/java/com/foco/app/ui/dashboard/DashboardStateTest.kt` —
      a stored day with a null count resolves to "absent", a stored `0` resolves to zero (red at
      step 1 today: both collapse to 0).
- [x] 4. Regression (device): `app/src/androidTest/java/com/foco/app/ui/UnlockCountTest.kt` — the
      counted case still shows the number next to the total; add the not-stored case showing no
      zero claim.
- [x] 5. `gradlew.bat test` green; `node scripts/gates/gate-check.mjs --run-tests` records evidence.

- [x] 6. Spec delta: `specs/usage-tracking/spec.md` — MODIFIED *A finished day keeps its unlock
      count* (+ the absent-not-zero scenario); `openspec validate dashboard-unlock-count-not-stored
      --strict` passes.

## Riskiest slice first

Task 1 (the coercion) — everything else follows from keeping the null.

## Deps

`dashboard-periodic-refresh-resilience` (parallel change owns `DashboardScreen.kt` first);
`dashboard-ring-content-survives-settings` and `week-row-opens-a-day-detail` chain after this one.
