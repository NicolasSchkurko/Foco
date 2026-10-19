# Proposal — The boundary check derives its feature folders from the tree

## Why

`LayerRulesTest` is the `[@test]` the living requirement declares for the cross-feature boundary
(`openspec/specs/code-structure/spec.md:61-63`), but it enforces that boundary only for a hardcoded set:

```kotlin
// app/src/test/java/com/foco/app/structure/LayerRulesTest.kt
L40:  private val uiFeatures = setOf("dashboard", "detail", "settings", "onboarding")
L87:  val feature = declaredPackage.split('.').getOrNull(4)
L88:  if (feature != null && feature in uiFeatures) {     // only those four
```

The rule the requirement writes is general: *"A file under a `ui/<feature>/` folder SHALL NOT import
another `ui/<feature>/` package"*, and its scenario (`:65-67`) is exactly "a screen imports a widget
that lives under another feature's folder → the boundary check fails and names the file".

**Mechanism of the hole.** A file `com.foco.app.ui.reports.X` importing `com.foco.app.ui.dashboard.Y`
produces `feature = "reports"`; `"reports" !in uiFeatures`, so the `if` is skipped, zero violations are
recorded and the test **passes**. The tripwire stops covering precisely the case the scenario
describes — a feature folder that did not exist when the check was written.

Today `ui/` holds exactly `common, dashboard, detail, metrics, onboarding, settings, theme`: the four
hardcoded ones plus the three the requirement exempts. So the hole is **latent, not active** — which is
why nothing has failed yet, and why it will only surface as a silent regression.

## What Changes

- The check derives the UI feature folders from the source tree: every folder under `ui/` except the
  shared ones — the `ui` root, `ui/common/`, `ui/metrics/`, `ui/theme/`.
- It asserts that derivation, so a folder cannot fall outside the rule again (the assertion fails
  against the hardcoded list *before* the fix, which is what makes it a regression).
- The delta settles the small ambiguity in the requirement: its parenthetical names the `ui` root,
  `ui/metrics/` and `ui/theme/` as shared but omits `ui/common/`, which the requirement's own first
  sentence establishes as the home of cross-screen reuse — so `common` is shared, never a feature.
- Nothing of the app changes: this is test-only.

## Alternatives Considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Derive the feature set from the tree, excluding the shared folders, and assert the derivation** — chosen (human, 2026-10-18: "the test is wrong") | The rule becomes what the requirement says; a new feature folder is covered without editing the check | Needs the shared list; it is the one thing that genuinely is a list |
| Keep the hardcoded list and add a comment telling future authors to extend it | Smallest diff | A tripwire that depends on remembering to extend it is not a tripwire; the rule would keep being unenforced |
| Treat every `ui/` sub-folder as a feature (no shared list) | No list at all | `common`, `metrics` and `theme` are legitimately imported by features, so every feature would fail — the check would be unusable |
| Enforce it in the gate script instead of the test | One place for structure checks | The requirement declares a `[@test]`, not a gate rule; moving it would leave the declared citation wrong |

## Impact

- `app/src/test/java/com/foco/app/structure/LayerRulesTest.kt` only
- Ordering: overlaps `cross-midnight-sessions`, `same-millisecond-transitions` and `themed-window` on
  the `app/src/test/**` glob — **no file in common** in any of the three. Only
  `datastore-test-on-windows` (already `verified`, so satisfied) is declared as a dep; declaring the
  two drafts would queue this small fix behind them for no collision. Reversible with one line.
- No protected zones, no new dependency, no platform API: **no tech brief**.
