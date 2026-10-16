# Proposal — Back arrow on the app detail screen

## Why

The user, on the emulator: "it doesn't let me go back … there should be some arrow to go back when I enter the app details." Measurement during `/spec-verify`: the system back action works (`KEYCODE_BACK` from the detail returns to the dashboard — verified with screenshots), but the detail screen shows **no visible way back**: on a gesture-navigation device, going back is undiscoverable.

## What Changes

- A visible **back arrow** (top-left) on the detail screen; tapping it returns to the dashboard, with the same behavior as the system back action.
- Non-goals: changing the navigation structure or other screens.

## Expected Outcome

"Volver siempre con un tap visible": from any app detail, one visible tap returns to the dashboard — no need to know the system gesture.

## Alternatives Considered

| Option | Summary | Why not chosen |
|---|---|---|
| **Arrow button in the detail header — chosen** | Minimal; fits the existing header; same action as system back | — |
| Material `TopAppBar` with back icon | Conventional layout | Restructures the screen's top area for a personal app |
| Rely on system back only | Zero UI | What the user just rejected (undiscoverable on gesture navigation) |

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `usage-tracking`: the detail SHALL offer a visible back control.

## Impact

- `app/src/main/java/com/foco/app/ui/dashboard/AppDetailScreen.kt` — back arrow wired to the current back dispatcher (no signature changes to `FocoNavHost`)
- `app/src/androidTest/**` — `AppDetailNavigationTest` extended with the arrow case
- Ordering (pre-declared under the A+ queue policy, flagged to the human): `app-settings` adds this change to its `x-deps` — this lands first (it is the direct response to the UX report); it overlaps `app-settings` on `ui/**` and `androidTest/**`
