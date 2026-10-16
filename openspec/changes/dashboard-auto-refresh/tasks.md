# Tasks

## 1. Dashboard auto-refresh

- [x] 1.1 Consolidate the data load on one trigger: reload whenever the screen reaches RESUMED (first show included) and add a RESUMED-scoped periodic loop (~30 s) that pauses when the app is not visible; verify manually on the emulator: open → data; leave to another app → return → numbers update without touching anything; wait 30 s → updates again.
- [x] 1.2 Remove the "Actualizar" button and its refresh counter from the header; verify the app builds (`gradlew.bat assembleDebug`) and the header renders without it.
- [x] 1.3 Add a small injectable seam for tests (refresh interval and data loader as parameters with production defaults); verify the app behavior is unchanged with the defaults (manual emulator check).

## 2. Instrumented test (first androidTest in the repo)

- [x] 2.1 Add the Compose UI test dependencies and the test runner configuration; verify an empty `gradlew.bat connectedDebugAndroidTest` run completes with the emulator on.
- [x] 2.2 Write `DashboardRefreshTest`: a RESUME transition triggers a load, a short injected interval triggers repeat loads while visible, and pausing stops the loop; verify with `gradlew.bat connectedDebugAndroidTest`.
- Note: instrumented runs need an **API ≤ 36 emulator** — Espresso crashes on API 37 (`NoSuchMethodException: android.hardware.input.InputManager.getInstance`, known issue, not ours). The repo's test AVD is `Medium_Phone_API_36.0` (created during this apply).

## 3. Integration

- [x] 3.1 Run the JVM suite (`gradlew.bat test`), the instrumented test, and the gate with green evidence (`node scripts/gates/gate-check.mjs --run-tests`); verify all pass and report the instrumented result separately (the gate's `test_command` covers JVM only).
- [ ] 3.2 On commit, update `x-gitref` in `.openspec.yaml` to the branch tip; verify it matches `git rev-parse --short HEAD`.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive dashboard-auto-refresh`.
- `x-deps: session-based-metrics` must be archived (or verified) before this change can reach `verified`.
