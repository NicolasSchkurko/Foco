# Proposal — Blind the permission gate (onboarding vs dashboard)

## Why

The user reported: "with the usage-access permission already granted, the onboarding screen still showed." The investigation could **not** reproduce it (cold start granted → dashboard; live revoke → onboarding; live grant → dashboard — all correct on the API 36 emulator, with screenshots). The observed failure class is explained by the gate's own design: it re-evaluates only on launch/return, so a stale composition after an external permission change (e.g. an emulator left mid-test on the onboarding) looks exactly like the bug; reinstalls also wipe the AppOp, making a previously granted permission silently disappear. The real gap: the entry gate has **no spec coverage and zero tests** — nothing states the contract and nothing would catch a regression.

## What Changes

- New living-spec capability `app-entry`: the permission-gated entry (granted → dashboard; missing → onboarding explaining the permission and linking to Settings) and its re-evaluation when the app returns to the foreground.
- Instrumented tests in two layers (human decision — "both"):
  - `AppEntryGateTest` — **E2E with real appops** (`appops set com.foco.app android:get_usage_stats …` via UiAutomation): cold start both ways, live transitions, and it **restores the permission at the end** (the artifact that started this: the emulator left mid-test).
  - `AppEntrySeamTest` — deterministic coverage via a minimal production seam: `App(isUsageAccessGranted = …)` with production default `UsageAccess::isGranted`; covers the resume flip in both directions and the onboarding content.
- No intended behavior change. If a test comes out red during the apply, the cause is fixed within this change (the regression test already exists by construction).

## Expected Outcome

"Flujo de permiso blindado por tests": the three paths (granted → dashboard at launch; live revoke → onboarding; live grant → dashboard) run green in instrumented runs; a future regression breaks a test instead of surprising the user.

## Alternatives Considered

| Option | Summary | Why not chosen |
|---|---|---|
| E2E with real appops — **chosen (layer 1)** | Tests the real AppOps → gate integration; no production surface | Alone: emulator-bound and less deterministic |
| Injectable seam — **chosen (layer 2)** | Deterministic, fast coverage of the gate logic and screen content | Alone: it would test a fake permission |
| **Both — chosen** (human decision) | Real integration plus deterministic logic coverage | Slightly more test surface |
| Reject/investigate further | The failure was not reproducible; nothing to fix on evidence | Leaves the contract unstated and untested |

## Capabilities

### New Capabilities

- `app-entry`: the permission-gated entry contract (onboarding ↔ dashboard, re-evaluated on return).

### Modified Capabilities

(none)

## Impact

- `app/src/main/java/com/foco/app/ui/App.kt` — seam parameter only (default preserves current behavior)
- `app/src/androidTest/**` — two new test classes (gate + seam)
- Sequencing: independent from `app-usage-detail` (human decision); their overlap on `App.kt`/`androidTest` stays informational — whichever applies second adapts
