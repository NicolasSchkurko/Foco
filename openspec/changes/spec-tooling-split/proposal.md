# Proposal — The repo's self-verification rules get a capability of their own

## Why

`code-structure` promises, in its name and its Purpose, to keep the **source tree** organized: one declaration per file, explicit layers with a pure core, shared widgets where they belong. Four of the nine requirements it now owns do not talk about the tree at all — they talk about **how this repository verifies itself**: a check that must be callable on a given root and prove its own failure branch, a check that must derive its scope from the tree instead of a hardcoded list, a JVM suite that must not fail because of the platform it runs on, and a configuration whose declared rules must not be silently dropped.

They landed in `code-structure` because their checks live under `app/src/test/java/com/foco/app/structure/`. Location decided ownership, and the result is a capability where four of nine live requirements nobody would look for. Two records already point at the mismatch: `spec-config-rules-quoted`'s Alternatives table keeps the option of a new capability and the human's 2026-10-18 choice to leave the rule in `code-structure`; and the 2026-10-19 ADR that drained the `code-structure` queue describes it as *"the capability that owns this repo's development rules (tripwires, artifact budgets, tooling config)"* — the description this change finally gives a name of its own, while recording that the structural fix was deferred.

## What changes

Create the living capability **`spec-tooling`** — the repo's rules for how it verifies itself and stays honest, never application behavior — and move into it the requirements that are about tooling, not about the tree:

| Requirement (and the change that brought it) | What it is about | Where it goes |
|---|---|---|
| One type per file (`layered-structure`) | tree shape | `code-structure` |
| Explicit layers with a pure core (`layered-structure`) | tree shape | `code-structure` |
| Shared UI widgets live in `ui/common/` (`structure-housekeeping`) | tree shape | `code-structure` |
| Reorganization preserves behavior (`structure-housekeeping`) | tree reorganization | `code-structure` |
| The day's query window is computed by a pure function (`day-window-seam`) | where the computation lives (layer) | `code-structure` |
| JVM tests are portable across development platforms (`datastore-test-on-windows`) | the tests | **`spec-tooling`** |
| The boundary check derives its feature folders from the tree (`layer-check-scope`) | a check | **`spec-tooling`** |
| A whole-tree check proves its own failure branch (`boundary-check-fixture`) | a check | **`spec-tooling`** |
| Declared rules stay effective (`spec-config-rules-quoted`) | the tooling's own configuration | **`spec-tooling`** |

So `spec-tooling` = the tripwires, the artifact budgets and the tools' configuration. What does **not** enter: anything about the application, and nothing about the **shape of the code** — one type per file, layers, where helpers live, the comment/prose budgets of the sweeps — that stays in `code-structure`, because it speaks about the tree.

- Four live requirements move from `openspec/specs/code-structure/spec.md` to the new `openspec/specs/spec-tooling/spec.md`, text and `[@test]` lines unchanged.

## Proof (a refactor changes no behavior)

Same requirements, same tests, no code touched: the contract is only re-homed. The existing suite is the proof, as the repo defines for a refactor: `LayerRulesTest` (the tree and the checks it runs), `SettingsMigrationTest` (the portable-runner rule) and `SpecConfigRulesTest` (the declared rules stay effective) stay green, and `openspec validate --all --strict` stays clean.

## Reversal

This reverses two recorded choices: the 2026-10-18 note in `openspec/changes/archive/2026-10-18-spec-config-rules-quoted/proposal.md` (keep the rule inside `code-structure`) and the deferral recorded in `DECISIONS.md` on 2026-10-19 (*draining the `code-structure` queue*, where the structural fix was deliberately deferred). The reversal is recorded as its own ADR in `DECISIONS.md` on 2026-10-19.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Create `spec-tooling` and move the tooling requirements** | One owner per fact: the tree rules stay under a name that promises the tree, and the self-verification rules get a home that says so | **Chosen (human, 2026-10-19)** |
| Keep everything in `code-structure` | No churn | A capability whose name and Purpose misdescribe half of what it owns; the 2026-10-18/2026-10-19 choices, now reversed |
| Rename `code-structure` instead of splitting | One capability, honest name | Merges two different facts — tree shape and self-verification — under one owner; relabels the confusion instead of removing it — **rejected** |

## Impact

- `openspec/specs/spec-tooling/spec.md` (new) and `openspec/specs/code-structure/spec.md` (four requirements removed); no code and no test is edited.
- `ARCHITECTURE.dsl`: no component realizes the tooling checks, so the gate will warn that `cap:spec-tooling` is untagged. Decide with `/diagram-sync` whether to model the checks as a development component or to accept the warning; this change does not invent one.
- No `deps`: nothing else in flight touches these spec files.
- Not a `tech_brief`: no code path, no state, no API, no dependency.
