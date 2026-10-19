# Tasks

## 1. The reflow (wording only — nothing may change meaning)

- [ ] 1.1 `app-settings` → *The appearance is selectable and persisted*: the window/relaunch clause leaves the SHALL sentence; the scenarios that already describe it keep it (*Switching to dark* names the activity window, *A language change keeps the themed window* is the relaunch case). Keep both `[@test]` lines and all four scenarios.
- [ ] 1.2 `code-structure` → *Explicit layers with a pure core*: keep the title (the archive merge matches by title) and **all five scenarios** — a MODIFIED block replaces the whole requirement, so dropping scenarios makes OpenSpec refuse the archive. The examples (the `availableDays` case, the raw platform markers, the import pattern) stay in the scenarios; only the SHALL sentence shrinks, keeping every rule it states.
- [ ] 1.3 `code-structure` → *Reorganization preserves behavior*: the storage sentence moves into the scenario *Stored settings survive the storage split*, which gains the idempotency clause. Keep both `[@test]` lines and all three scenarios.
- [ ] 1.4 Sanity: every scenario of the three requirements exists exactly once after the reflow; no requirement is dropped, renamed or re-scoped; the wording of each rule is preserved (examples may move, rules may not).

## 2. Verify (the validator is the oracle)

- [ ] 2.1 `openspec validate --all --strict` **before** the change: record the two failing items (`app-settings`, `code-structure`) and the three width warnings — that is the defect being fixed.
- [ ] 2.2 `openspec validate --all --strict` **after**: no width warning anywhere (the 25 other requirements were already inside the budget), no new failure.
- [ ] 2.3 `openspec validate spec-requirement-budget --strict`: the delta itself is valid (its own texts must also fit the budget).
- [ ] 2.4 `gradlew.bat test` green — the tests the reflowed requirements declare are the regression proof: `LayerRulesTest`, `SessionBuilderTest`, `SettingsMigrationTest`, `AppAppearanceTest`.
- [ ] 2.5 `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, then archive with `gate-check --pre-archive spec-requirement-budget` — the living spec files change at **archive** time, so the branch itself carries only this change's folder.
- Order: `spec-config-rules-quoted` declares a dep on this change (both land a delta in `code-structure`), so they are applied one at a time, never on two branches at once.
- Out of scope (reported in the proposal): `code-structure`'s Purpose sentence still does not mention the repo's tooling configuration, and an OpenSpec delta cannot edit a Purpose.
