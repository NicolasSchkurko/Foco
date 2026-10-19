# Proposal — The boundary check proves its own failure branch

## Why

`/spec-verify` on `layer-check-scope` (2026-10-19) filed one medium finding: the scenario it declares says *"the boundary check fails and names the file"*, but **no test ever makes the check fail**. Both tests in `LayerRulesTest` only assert that the *real* tree produces no violations (`app/src/test/java/com/foco/app/structure/LayerRulesTest.kt:62-133`) or classify folder names (`:135-152`). So the rule this repo leans on — *a `ui/<feature>/` file must not import another feature's package* — has only ever been proven **silent**, never proven **to fire**. That is the same shape as the defect `layer-check-scope` just fixed: a tripwire whose business branch nobody exercises will rot unnoticed.

There is a second reason it is cheap now: the check derives its feature folders from the tree, so a fixture only has to be *a tree that violates the rule* — no list to update, no production code involved.

## What Changes

- The tree scan moves out of the `@Test` into `violationsIn(sources: File): List<String>` — the same rules, the same walk, with the root as a parameter. The existing test keeps asserting that the real `src/main/java` yields none, so nothing about today's behavior changes.
- A fixture test builds a **synthetic tree** in a JUnit `TemporaryFolder` (the pattern `SettingsMigrationTest` already uses) with three folders under `com/foco/app/ui/`: `dashboard` (a plain widget), `reports` (a file importing `com.foco.app.ui.dashboard.…`) and `common` (a file doing the same). It asserts the scan reports exactly one violation, on the `reports` file, naming it — and none for the `common` one.
- Nothing else moves: no app code, no rule weakened, no new dependency.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Parameterize the scan by root + a `TemporaryFolder` fixture** | Proves the failure branch end to end (the real walk, the real rule), with no production code and no folder added to the repo; reuses the `TemporaryFolder` pattern in-tree | **Chosen** |
| Leave it as the finding, unproven | Zero work | The medium finding stays: the rule's failure branch keeps being untested, and the scenario keeps over-claiming — **rejected** |
| Assert only the derivation (`isUiFeature("reports")`) | Cheapest | That is what the change already does, and it is exactly what does *not* prove the check fires — **rejected** |
| A real violating file (or folder) committed under `app/src/test/**` and scanned | No temp folder, no parameter | Pollutes the repo: a Kotlin file that imports another feature on purpose, and `OneTypePerFile`/`LayerRulesTest` itself would police it — **rejected** |
| Move the check into the gate script | One place for structure checks | The requirement declares a `[@test]`; the fixture belongs where the rule lives — **rejected** |

## Impact

- `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt` only. Test-only: **no tech brief**, and the comprehension pass is not required before archiving.
- `deps: [layer-check-scope]`, and this one is a **real ordering**: this change edits the file that change rewrites, so it must be applied *after* `layer-check-scope` is merged (`verified` alone would let the branch be cut from a `main` that still holds the hardcoded list). No protected zones, no new dependency.
- Kind is `refactor`, not `fix`: no behavior changes — a scan is moved (`LayerRulesTest.kt:62-133`) and fixtures are added; the green suite plus the new fixture is the proof. The finding it closes is a *gap in proof*, not a misbehaving check.
