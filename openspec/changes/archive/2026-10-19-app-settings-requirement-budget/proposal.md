# Proposal — app-settings-requirement-budget

## Why

Archiving `settings-about-and-pickers` brought its dropdown wording into the living `app-settings`
spec, and the appearance requirement now crosses the 500-character budget the strict validator
enforces (`openspec validate --all --strict` reports 7 passed, 1 failed). The behavior is right; the
paragraph packs a detail that a scenario already pins.

## What Changes

- `openspec/specs/app-settings/spec.md` — "The appearance is selectable and persisted": the prose keeps
  the row, the dropdown, the immediate application, the persistence and the system-following rule, and
  drops the parenthetical about the activity window, which the "Switching to dark" scenario states.
- Every existing scenario is copied unchanged; both `[@test]` lines stay.

## Non-goals

- No behavior change and no code change: the screen and its tests are untouched.
- No change to the language requirement, which is under the budget.

## Impact

- The change's target is the spec file itself (the `spec-requirement-budget` precedent): this is a
  contract edit, and its proof is the existing instrumented and JVM tests staying green.
- No protected zone (`openspec/config.yaml` → `protected: []`).
