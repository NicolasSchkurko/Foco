# Proposal — Themed window: the chosen appearance governs the app's window too

## Why

Two user-visible flashes after `app-settings` landed (reported 2026-10-16; reproduced on the API 36 AVD in every combination — light and dark systems, in-app Oscuro over a light system):

1. **"Al cambiar de idioma se pone todo en negro por unos segundos."** Switching the language calls `AppCompatDelegate.setApplicationLocales` → the activity is recreated (configuration change). During the relaunch the visible background is the activity theme's window background (`Theme.Foco` = `Theme.AppCompat.DayNight.NoActionBar`), which follows the **system** uiMode — near-black on a dark system — until Compose paints again.
2. **"Las transiciones en modo oscuro tienen como un fade en blanco."** With Oscuro picked on a light system, Compose paints dark but the **window** stays light: transitions and the relaunch show that light background behind the dark UI.

Single root cause: the in-app appearance never reached the window/theme layer — it only resolved Compose's `darkTheme` (`MainActivity.kt:34`), and nothing calls `AppCompatDelegate.setDefaultNightMode` (grep: zero).

## What Changes

- **A — the appearance reaches the theme**: `data/AppAppearance.kt` maps Apariencia → appcompat night mode (AUTO→FOLLOW_SYSTEM, Claro→NO, Oscuro→YES) and applies it; `MainActivity` applies the stored value at startup (in the same pre-content read as the language) and reacts to later picks. Activity theme, resources and Compose stay in the same scheme.
- **B — the window matches the pixels**: the activity window background follows `MaterialTheme.colorScheme.surface` (dynamic colors included) through a named effect, so a relaunch or a transition never reveals the opposite scheme.

Acceptance: no flash of the opposite scheme anywhere (picking a language, picking an appearance, cold start with a stored override); the relaunch gap, if visible at all, shows the chosen scheme.

## Alternatives Considered

| Option | Why yes | Why not / status |
|---|---|---|
| **B only** (window background = Compose color) | No extra recreation when picking an appearance; exact color | Doesn't align the theme/resources nor the static window before the first composition — incomplete — **rejected** |
| **A only** (`setDefaultNightMode`) | Canonical appcompat; theme, resources and window follow the choice | Dynamic colors match only approximately; the static window can lag a hair — good but improvable — **not chosen** |
| **D — `android:configChanges="locale|uiMode"`** | Removes the relaunch gap on language changes entirely | Fights appcompat's model; risks resources/Compose going stale — **rejected** |
| **A+B** — chosen (human, 2026-10-16) | Fixes the root (theme) and the pixels (window); each piece is small | Touches platform SDKs → `tech_brief: true` + walkthrough + comprehension sign-off |

## State and persistence mechanism

No new storage: the fix reads the existing `appearance` preference (DataStore `foco_settings`, owned by `app-settings`) at startup and on change. The window background is derived at runtime from the Compose color scheme — nothing new is persisted.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `app-settings`: "The appearance is selectable and persisted" now covers the activity window and the relaunch/transitions (no opposite-scheme background).

## Impact

- `app/src/main/java/com/foco/app/MainActivity.kt` — startup + on-change application; window background effect
- `app/src/main/java/com/foco/app/data/**` — `AppAppearance` (platform wrapper)
- `app/src/test/**` — `AppAppearanceTest` (mapping)
- `app/src/androidTest/**` — `SettingsScreenTest` asserts the applied night mode
- Ordering: waits for `app-entry-gate` and `structure-housekeeping` (declared deps)
