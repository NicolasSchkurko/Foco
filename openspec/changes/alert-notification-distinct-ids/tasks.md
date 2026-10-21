# Tasks — alert-notification-distinct-ids

- [x] 1. Delta `specs/app-limits/spec.md`: ADDED *One notification per app* — per-package id, side
      by side, `Entendido` dismisses its own, same-app tier updates stay on that app's id.
- [x] 2. `AppLimitAlertNotifier.kt:102`: derive the id from the package without the `% 1000`
      truncation (full hash or equivalent stable scheme) — two packages must never share an id.
- [x] 3. Regression: `app/src/androidTest/java/com/foco/app/data/AppLimitAlertNotifierTest.kt` —
      two packages engineered to collide under `hash % 1000` produce two standing notifications
      (today: one replaces the other); `Entendido` removes only its own.
- [x] 4. `gradlew.bat test` green; `gate-check --run-tests` records evidence; `openspec validate
      alert-notification-distinct-ids --strict` passes.

## Riskiest slice first

Task 3's engineered-collision test (red against the `% 1000` scheme), then task 2.

## Deps

None. No other active change claims `AppLimitAlertNotifier.kt`.
