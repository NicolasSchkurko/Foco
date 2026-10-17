# Walkthrough — structure-housekeeping (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them.

## Mechanism

Housekeeping, zero behavior change. **(1)** Settings get their own Preferences DataStore:
language and appearance move from the shared file `excluded_apps` to a new file
`foco_settings` (declared by `SettingsStore`); the legacy file goes back to holding only the
excluded packages (`ExcludedAppsStore`). **(2)** Because existing installs already have a
language/appearance saved in the legacy file, a **one-shot migration** runs at startup,
before anything reads the language: it copies `language` and `appearance` into the new file
and removes them from the legacy one; nothing else in the legacy file is touched.
**(3)** Shared UI widgets find a neutral home: `AppIcon`, `appLabel` and `HourChart` move to
`ui/common/`, the detail screen to `ui/detail/`, and `LayerRulesTest` pins the boundary (UI
features must not import each other). Expressivity pass on the storage files: short
physical-resource headers; `ExcludedAppsStore.add/remove` renamed to `exclude/restore`.

## Physical traceability

- `excluded_apps` (app-private DataStore file): the excluded-packages set only
  (`excluded_packages`). Written by the dashboard long-press/undo and the Settings restore.
- `foco_settings` (new, same storage): `language` (`auto`/`es`/`en`) and `appearance`
  (`auto`/`light`/`dark`). Written when the user picks an option; read by `MainActivity`
  (startup) and the Settings screen.
- `data/SettingsMigration.kt`: `migrateSettingsFromLegacyDataStore(context)` — copy → clear;
  idempotent; early-returns when the legacy store has no setting keys (fresh installs create
  nothing).
- Lifecycle: both files live until uninstall/clear-data. No network.

## Limits and failure modes

- Killed between the copy and the cleanup → the next startup copies again (same values;
  harmless) — idempotent by construction.
- Legacy file missing/empty → early return; defaults unchanged (`AUTO` = system).
- Cost: one small extra file read on the first startup after the update, then a no-op check.
- Byte-corrupt preference files are not self-healed (no corruption handler) — unchanged from
  before.

## Touched APIs

- Preferences DataStore (existing dependency): a second file, same API. Nothing else — no
  new library, no new platform API.

## Implicit decisions

- One-shot copy at startup in `MainActivity` (before the language apply) instead of lazy
  migration on first read — simplest place; runs once; the rest of the app stays oblivious.
- The migration copies only `language`/`appearance`; unknown values travel verbatim and are
  interpreted by the existing `fromWire` fallback.
- `ui/common` also received `HourChart`: the compiler plus the new boundary rule forced it —
  it is genuinely shared by dashboard and detail. The rule working as designed.
- Upgrade-over-an-old-build on a device: covered by the JVM migration tests + the
  instrumented suite; a manual upgrade pass is optional for the human.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. Where do the settings live now, where did they live before — and who moves them, and at
   what moment? — TODO(human)
2. What happens if the app is killed exactly between the copy and the cleanup — and why is
   that acceptable? — TODO(human)
3. To add a new preference to Settings (say, a fourth option), which files and functions do
   you touch? — TODO(human)

When satisfied, the sign-off is recorded in `/spec-verify` (`understood_by` + `understood_at` in
`.openspec.yaml`).
