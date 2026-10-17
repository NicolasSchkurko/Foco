# Proposal — Structure housekeeping: shared widgets in `ui/common`, the detail screen in `ui/detail`, settings on their own DataStore

## Why

Two spots in the tree no longer match the constitution's expressivity/structure rules, and one of them carries a naming debt from a previous decision:

1. `AppIcon`/`AppLabel` (both in `ui/dashboard/`) are shared by the dashboard, the app detail and now Settings (`ui/settings/SettingsScreen.kt:41-42`), so a feature package owns widgets another feature depends on. `AppDetailScreen` also lives under `ui/dashboard/` although it is its own screen.
2. `data/AppPreferences.kt` is a single DataStore file (`excluded_apps`, historical name) holding three domains — exclusions, language and appearance. The name no longer says what it holds, and sharing one file forces every settings change to reason about the exclusions' storage.

Housekeeping: no observable behavior changes; the proof is the green suite plus a regression test for the storage split.

## What Changes

- **Shared widgets to `ui/common/`**: `AppIcon` + `appLabel` move from `ui/dashboard/` to `ui/common/`; `AppDetailScreen` moves from `ui/dashboard/` to `ui/detail/`; imports updated (dashboard rows, detail, settings, `FocoNavHost`).
- **Settings get their own DataStore**: language + appearance move to a new file (`foco_settings`) declared by `SettingsStore`; `excluded_apps` goes back to being the exclusions' store only. A one-shot **migration** at startup copies `language`/`appearance` from the legacy store into the new one and clears them there (idempotent); fresh installs have nothing to migrate.
- **Expressivity pass (per the constitution)**: the storage files get their short physical-resource header; `ExcludedAppsStore.add/remove` become `exclude/restore` (the class names the domain, the method names the effect); `data/AppPreferences.kt` disappears (each store declares its own DataStore). The `ui/` boundary gets pinned by `LayerRulesTest`: a feature package (`ui/<feature>/`) must not import another feature's package — shared widgets live in `ui/common/`.

## Alternatives Considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Status quo**: one shared DataStore + widgets in `ui/dashboard` | Zero work; already documented | The file name lies (3 domains) and widget ownership forces cross-feature imports — **rejected** (human decision, 2026-10-16) |
| **Split WITHOUT migrating** (fresh `foco_settings`, legacy keys ignored) | Simplest; least code | Loses the user's chosen language/appearance on upgrade — a visible regression — **rejected** |
| **Rename the single file with migration** (`app_preferences`) | One file, honest name | Still mixes domains; migration machinery for no user gain — **rejected** |
| **Split with one-shot migration** — chosen | Honest names per domain, invisible to the user; the migration is small and testable (JVM, temp files) | Touches persistence → `tech_brief: true` + walkthrough + comprehension sign-off |

## State and persistence mechanism

Two app-private files, created by the app on first write:

- `excluded_apps` (unchanged): the excluded-packages set (`excluded_packages`). Written by the dashboard's long-press (with undo) and the Settings restore.
- `foco_settings` (new): `language` (`auto`/`es`/`en`) and `appearance` (`auto`/`light`/`dark`). Read by `MainActivity` (startup) and the Settings screen; written when the user picks an option.

Migration: at startup, before the language is read, a one-shot pass copies `language`/`appearance` from `excluded_apps` to `foco_settings` and removes them from `excluded_apps`. It is idempotent (nothing left to copy afterwards) and safe on fresh installs (no keys found). Legacy file gone/unreadable → defaults (`AUTO`), as today.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `code-structure`: gains the shared-UI boundary requirement (shared widgets in `ui/common/`, no cross-feature imports) and extends "Reorganization preserves behavior" to storage splits.

## Impact

- `app/src/main/java/com/foco/app/ui/**` — moves + imports (`common`, `detail`, `dashboard`, `settings`, `FocoNavHost`)
- `app/src/main/java/com/foco/app/data/**` — `SettingsStore` (own store), `ExcludedAppsStore` (header + renames), the migration, `AppPreferences.kt` removed
- `app/src/main/java/com/foco/app/MainActivity.kt` — migration call before the first language read
- `app/src/test/**` — new `SettingsMigrationTest` (copy/clear/idempotence over temp-file stores); `LayerRulesTest` extended with the cross-feature boundary
- `app/src/androidTest/**` — call sites of the renamed store methods only
- Regression proof: full suite green (JVM + instrumented); nothing observable changes
- Ordering: `app-entry-gate` applies first (declared dep; overlap on `androidTest/**`)
