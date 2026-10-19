# Tasks

## 1. The headers

- [x] 1.1 Detail header (`AppDetailScreen`): the back control moves to the header's leading edge (4 dp inset) with the app icon + name centered in the row (balancing space against the arrow).
- [x] 1.2 Settings header (`SettingsScreen`): same treatment (arrow at the edge, title centered).
- [x] 1.3 Regression: `AppDetailNavigationTest` + `SettingsScreenTest` assert the back control's left bound (≤ 24 dp from the edge; ≈32 dp before the fix) — they fail without the change.
- [ ] 1.4 Visual pass (AVD/device): both headers in light and dark; the arrow reads at the edge and the identity/title reads centered; back keeps working (tap, key, gesture).

## 2. Integration

- [x] 2.1 `gradlew.bat test` + `connectedDebugAndroidTest` (API 36 AVD) + `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive back-arrow-inset`.
- Ordering: waits for `app-entry-gate` → `structure-housekeeping` → `themed-window` (declared deps).
- No tech brief (visual-only): the comprehension pass is not required before archiving.

### Notes on the apply (2026-10-19)

- **Structure:** both headers are now a full-width `Box`: the `IconButton` aligned `CenterStart` with a 4 dp leading inset, and the middle content (detail: app icon + name + day usage; settings: title) aligned `Center`. The horizontal 20 dp padding moved off the header and onto the content below it (settings: the `LazyColumn`; detail: the `when` branch that renders the chart, summary and sessions) so the rest of the screen keeps its padding.
- **Measured, not assumed:** with the old inset simulated (20 dp), the assertion failed with the semantic node at `left=24.38 dp`; with the new inset the node sits at ~8.4 dp (the glyph ~8 dp further in: ~16 dp against the old ~32 dp). The task's `≤ 24 dp` threshold left only **0.38 dp** of margin, so both assertions now use **`≤ 16 dp`** — about 8 dp of margin on each side, and the number is recorded in the test's message.
- **Both assertions discriminate:** with the 20 dp inset the settings one fails (`the arrow must sit at the header's leading edge (the old inset left the node at ~24 dp): DpRect(left=24.380953.dp, …)`); with the change, both pass.
- Instrumented suite on the API 36 AVD: **20/21** — `AppDetailNavigationTest` 3/3 (the new positional test included) and `SettingsScreenTest` 5/5 (the new positional test included, plus the language and appearance ones); the only red is `UnlockCountTest`, the known environmental one (the headless AVD reports 0 unlocks).
- **1.4 (visual pass) is NOT done**: nobody looked at the screens. The geometry and the three ways back are covered mechanically (the positional assertions and the tap/gesture tests), but "the arrow reads at the edge and the identity reads centered" in light and dark is still an eye check — left open on purpose rather than claimed.
