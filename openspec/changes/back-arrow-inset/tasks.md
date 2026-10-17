# Tasks

## 1. The headers

- [ ] 1.1 Detail header (`AppDetailScreen`): the back control moves to the header's leading edge (4 dp inset) with the app icon + name centered in the row (balancing space against the arrow).
- [ ] 1.2 Settings header (`SettingsScreen`): same treatment (arrow at the edge, title centered).
- [ ] 1.3 Regression: `AppDetailNavigationTest` + `SettingsScreenTest` assert the back control's left bound (≤ 24 dp from the edge; ≈32 dp before the fix) — they fail without the change.
- [ ] 1.4 Visual pass (AVD/device): both headers in light and dark; the arrow reads at the edge and the identity/title reads centered; back keeps working (tap, key, gesture).

## 2. Integration

- [ ] 2.1 `gradlew.bat test` + `connectedDebugAndroidTest` (API 36 AVD) + `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive back-arrow-inset`.
- Ordering: waits for `app-entry-gate` → `structure-housekeeping` → `themed-window` (declared deps).
- No tech brief (visual-only): the comprehension pass is not required before archiving.
