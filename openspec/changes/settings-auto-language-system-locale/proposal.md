# Proposal — settings-auto-language-system-locale

## Why

Foco supports Android 13+'s per-app language (the manifest publishes `localeConfig`,
`AndroidManifest.xml:40`, and `autoStoreLocales=false`), but every start applies
`AppCompatDelegate.setApplicationLocales(getEmptyLocaleList())` (`AppLanguage.kt:14`, called from
`MainActivity.kt:57`) — an **empty** list, i.e. a reset to the device default. A language the user
chose for Foco **in the system settings** is therefore wiped at the next launch, and the in-app row
shows `Automático` while the system believes otherwise.

The specs are silent (🟡): `app-settings:24-47` says "Automático SHALL follow the device language"
but never says whose default wins when the system carries a per-app choice for Foco — the row, the
reset behavior and the system setting can each mean something different, and the code picked the
reset by accident.

## What changes (proposed contract)

- MODIFIED requirement in `app-settings`: choosing/staying on Automático follows the device
  language **without clearing a per-app language chosen for Foco in the system settings**, so the
  system setting, the row and the applied locale agree.
- Implementation: stop applying an empty locale list over an existing system choice (or mirror the
  system answer); depends on the open decision below.

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Defer to the system per-app choice on Automático (never wipe it) | system setting survives; row mirrors it | **Proposed** |
| In-app Automático resets to device default | simple, but silently deletes the system choice — the reported defect | not proposed |
| Drop `autoStoreLocales=false` and let AppCompat own the state | framework behavior changes under the app (parity, activity relaunch) — bigger surface | not proposed |

## Risks

- The wipe mechanism is a partially confirmed hypothesis (found by inspection, not yet reproduced):
  the **first apply task reproduces it on a device** (set per-app language in system settings →
  launch Foco → language reset). If it does not reproduce, the change narrows to the contract text
  and the row's mirror behavior.
- The framework's precedence (empty list vs stored system value) differs across AppCompat versions
  — pinned by `MainActivityStartupTest` on the supported floor.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 10, severity medium, 🟡 spec-silent,
mechanism partially hypothesized); `kind: feature` via /spec-add with one open decision. Tech brief
kept (platform SDK locale APIs at startup). Awaiting human acceptance — no code written.
