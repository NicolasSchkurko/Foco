# Tasks — week-row-opens-a-day-detail

- [ ] 1. Decision recorded by human (today) — this is a documentation-only change: the week's app rows
      already open today's detail (`DashboardScreen.kt:517`, route `null = today`, DECISIONS.md:352);
      the contract is what is being added, not the behaviour.
- [ ] 2. Delta `specs/usage-tracking/spec.md`: ADDED *A week app row opens today's detail* — states
      that the week position's rows open today, that dated day views keep opening their own day, and
      a scenario that pins the current behaviour; `openspec validate week-row-opens-a-day-detail
      --strict` passes.
- [ ] 3. Regression (contract pin): `app/src/androidTest/java/com/foco/app/ui/WeekScreenTest.kt` —
      tapping a week row for an app used across several days opens today's detail (this freezes the
      chosen behaviour so it cannot drift).
- [ ] 4. `gradlew.bat test` green (this change touches no main code; test guards the contract only);
      `gate-check --run-tests` records evidence.

## Riskiest slice first

Task 2's requirement text — it is the only behavioural claim this change makes.

## Deps

`dashboard-ring-content-survives-settings` (end of the `DashboardScreen.kt` chain);
`dashboard-periodic-refresh-resilience` → `dashboard-unlock-count-not-stored` → this position.
