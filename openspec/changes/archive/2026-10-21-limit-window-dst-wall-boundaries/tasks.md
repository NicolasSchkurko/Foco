# Tasks — limit-window-dst-wall-boundaries

- [x] 1. `AppLimitWindowRules.kt`: replace the epoch arithmetic at `:84-90` and `:57-58` with a
      boundary resolver that maps (local date, stored minute, zone) → instant; keep `isInside`
      (`:28-46`) as the single wall-clock definition it already is.
- [x] 2. Decide and pin the spring-forward gap rule (stored minute inside the skipped range →
      first existing instant after the gap) and the fall-back repeated-hour counting rule.
- [x] 3. Regression: `app/src/test/java/com/foco/app/domain/AppLimitWindowRulesTest.kt` —
      spring-forward case (budget stops at 06:00 wall, not 07:00), fall-back case, boundary
      one-shot instant on both, and the unchanged normal-day results (today's green stays green).
- [x] 4. Spec delta: `specs/app-limits/spec.md` adds *Window boundaries follow the local wall
      clock* with the DST scenarios; `openspec validate limit-window-dst-wall-boundaries --strict`
      passes.
- [x] 5. `gradlew.bat test` green; `node scripts/gates/gate-check.mjs --run-tests` records evidence.

## Riskiest slice first

The spring-forward assertion of task 3 written **first** (red against `:84-90`), then task 1 makes
it green without moving the normal-day numbers.

## Deps

None. No other active change claims `AppLimitWindowRules.kt`.
