# Proposal — Fix the system back gesture + a proper back control on the detail

## Why

The user reported: the system **back gesture** on an app detail does not return to the dashboard (the back key and the visible control do — both verified green). Root cause (with evidence): the app targets Android 16 (API 36), where **predictive back is enabled by default**; the app never declares `android:enableOnBackInvokedCallback`, so the gesture's `OnBackInvokedCallback` is not registered by the activity and never reaches the `onBackPressedDispatcher` our navigation uses. Additionally, the back control is a text glyph (`←`) — the user asked to improve it.

## What Changes

- Declare `android:enableOnBackInvokedCallback="true"` on the application (the documented Android 16 migration step): the gesture then dispatches through the same path as the back key and the visible control.
- Replace the glyph with the Material icon `Icons.AutoMirrored.Filled.ArrowBack` plus `contentDescription = "Volver"` (accessibility); adds `material-icons-core` to the catalog/module if the build demands it.
- Regression tests: a JVM manifest-contract test (the flag is required) + the navigation e2e updated to find the control by its content description.

## Expected Outcome

The system back gesture returns from a detail to the dashboard exactly like the other two paths, and the control looks and reads like a standard Material back button.

## Alternatives Considered

| Fix | Summary | Why not chosen |
|---|---|---|
| **Manifest `enableOnBackInvokedCallback` — chosen** (human decision) | The documented migration; one line; routes the gesture into the already-tested dispatcher path | — |
| Bump `androidx.activity` | Some versions register the callback by default | Dependency churn, uncertain version; does not fix the cause (the flag is the fix) |
| Full predictive-back migration (animations) | The most polished | Overkill for a personal app; the flag already enables platform behavior |

| Control | Summary | Why not chosen |
|---|---|---|
| **Material `AutoMirrored ArrowBack` icon — chosen** (human decision) | Standard icon, correct size/tint, accessible | May add `material-icons-core` |
| Styled `←` glyph | No dependencies | Amateur look; no accessibility semantics |

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `usage-tracking`: the system back gesture returns from the detail; the back control is a standard accessible icon.

## Impact

- `app/src/main/AndroidManifest.xml` — `enableOnBackInvokedCallback`
- `app/src/main/java/com/foco/app/ui/dashboard/AppDetailScreen.kt` — icon swap
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — `material-icons-core` if needed
- `app/src/test/**`, `app/src/androidTest/**` — manifest contract test + updated navigation test
- Ordering (pre-declared under the A+ policy): `app-settings` adds this change to its `x-deps` — this lands first (user-reported bug); `app-entry-gate` follows transitively
