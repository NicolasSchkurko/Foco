# Proposal — Living requirements fit the tooling's text budget

## Why

OpenSpec's strict validation warns when a requirement's text is very long — measured as the SHALL paragraph **plus its `[@test]` lines** — and the repo has three living requirements over that line. `openspec validate --all --strict` therefore fails today, and a failure that is always there stops being read:

| Capability | Requirement | text | text + `[@test]` |
|---|---|---|---|
| `app-settings` | *The appearance is selectable and persisted* | 403 | **544** |
| `code-structure` | *Explicit layers with a pure core* | 589 | **658** |
| `code-structure` | *Reorganization preserves behavior* | 387 | **529** |

The first one is a side effect of archiving `themed-window` (2026-10-18): the delta merged its window clause into that requirement and took it from 265 to 544. The other two were already over. The other 25 living requirements sit between 123 and 417, so the contract is otherwise inside the budget.

The repo's gate (`scripts/gates/gate-check.mjs`) does not read this rule, so nothing caught it and nothing will catch the next one — that is why this is a `fix` and not a style nit.

## What changes

- `app-settings` → *The appearance is selectable and persisted*: the window clause leaves the SHALL sentence and lands in the scenarios that already describe it (*Switching to dark* says the activity window follows; *A language change keeps the themed window* is the relaunch case). Wording, not meaning: same `[@test]` lines.
- `code-structure` → *Explicit layers with a pure core*: the examples and the edge cases leave the SHALL sentence and stay in the scenarios — the five it already has cover purity, pure helpers, raw platform reads, the folder rule and the dependency direction. Title unchanged, `[@test]` unchanged, no requirement split: a MODIFIED block replaces the whole requirement, so nothing may be dropped from it (OpenSpec refuses to archive that).
- `code-structure` → *Reorganization preserves behavior*: the storage sentence (split a store → migrate, idempotently) moves into the scenario that already covers it (*Stored settings survive the storage split*), which gains the idempotency clause.

Nothing else moves: no requirement is added, removed or re-scoped semantically, and no test changes.

## Regression (why these `[@test]` lines)

`kind: fix` needs a regression test, and for a **wording** fix the honest proof is the tests the requirements already declare — they are what would break if the meaning had drifted: `SettingsScreenTest` + `AppAppearanceTest` (the appearance requirement) and `LayerRulesTest` (both `code-structure` requirements). The oracle for the budget itself is **OpenSpec's validator**, not a copy of its rule: the task is to show `openspec validate --all --strict` clean before the change turned in the report, with no warnings left in the living specs.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Reflow the three, keep every meaning in a scenario, cite the requirements' own tests** | Makes `validate --all --strict` clean; text budget respected where the tool asks; zero semantic change; no new file | **Chosen (human, 2026-10-18)** |
| Reflow only `app-settings` (the one my archive broke) | Smallest diff, matches the incident | `code-structure` would still fail strict validation, so the failure stays and the signal is lost — **rejected** |
| Add a JVM lint that recomputes the 500-character rule | Enforced by `gradlew.bat test` | Duplicates another tool's rule and its counting (text + `[@test]` lines) as of 1.14.3; when openspec changes it, the lint lies. The human chose the validator as the oracle — **rejected** |
| Split `code-structure`'s purity rule into its own requirement **and** rename the parent | Cleanest titles | Renaming makes the archive merge look for a title that no longer exists; the parent keeps its title on purpose — **rejected** |

## Impact

- `openspec/specs/app-settings/spec.md`, `openspec/specs/code-structure/spec.md` (the delta lands there at archive time; the branch itself carries only this change's folder).
- **No deps**: no active change claims a living spec file (`layer-check-scope` and `datastore-test-on-windows` have `code-structure` deltas, but their `targets` are code paths, and a delta only collides with another change's capability when it declares `provides:` — this one does not).
- Interaction to respect at apply time: `spec-config-rules-quoted` also lands a delta in `code-structure`, so that change declares a dep on this one and the two are applied in order, never on two branches at once.
- Not a `tech_brief`: no code, no state, no API.
- Out of scope: `code-structure`'s **Purpose** sentence still does not mention the repo's tooling configuration — the requirement added by `spec-config-rules-quoted` sits under it anyway (human's choice, 2026-10-18), and an OpenSpec delta cannot edit a Purpose.
