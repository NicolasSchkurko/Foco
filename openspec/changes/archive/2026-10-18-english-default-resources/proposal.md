# Proposal — The default (base) resources are English, not Spanish

## Why

`app/src/main/res/values/strings.xml` — the **base** resource file, the one Android uses when the device language has no dedicated resources — is written in **Spanish**: `settings_option_dark` = "Oscuro", `settings_title` = "Ajustes", `dashboard_title` = "Hoy", `loading` = "Cargando…". English lives in `values-en/` (the same 37 keys, byte-for-byte in step with the base today) and **there is no `values-es/` at all**. Two consequences:

1. Any device whose language is neither Spanish nor English (French, German, Portuguese…) falls back to the base file and shows **Spanish** — the app's default text is Spanish.
2. Spanish is not a real translation: it shows up only because it *is* the fallback. Add a third language, or move the base to English without adding `values-es/`, and "Español" silently disappears.

Android's model is the opposite of what the tree does: the base file is the fallback language (English here) and every other supported language lives in its own locale folder.

## What changes

- `values/strings.xml` (base) carries the **English** text — the values, key names, order and section comments that `values-en/strings.xml` has today.
- `values-es/strings.xml` (new) carries the **Spanish** text the base file has today, so Español resolves as a real locale instead of riding the fallback.
- `values-en/strings.xml` is **deleted**: once the base is English it is a duplicate, and two English copies drift apart (that is the shape of the bug being fixed).
- A JVM tripwire — `DefaultLocaleResourcesTest` — fails if the base goes back to Spanish, if the `es` resources disappear, or if base and `es` stop declaring the same key set.

Nothing else moves: no state, no dependency, no API, no code line. The language preference stays where it is (DataStore `foco_settings`, owned by `app-settings`), and `locales_config.xml` keeps offering `es` + `en`.

## Acceptance

- A device (or AVD) in a language with no resources of its own — e.g. French — shows the app in **English**.
- Español shows Spanish and English shows English, exactly as today; Automático still follows the device.
- The tripwire is **red before** the change (the base holds Spanish, `values-es/` does not exist) and green after: that failure is the regression proof.
- `SettingsScreenTest` stays green: it selects the Spanish UI by its Spanish text (`"Ajustes"`, `"Idioma"`), which after this change resolves from `values-es/` — so the instrumented suite is the second guard on the Spanish side.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Base → English, Spanish → `values-es/`, delete `values-en/`** | The base becomes the honest fallback; Español becomes a real locale; one English copy, no drift; smallest reviewable diff (two files + one new) | **Chosen (human, 2026-10-18)** |
| Base → English, Spanish → `values-es/`, keep `values-en/` | Keeps an explicit `en` folder, which some teams prefer | Two identical English copies that must be updated in lockstep; a string added to only one of them recreates today's bug shape — **not chosen** |
| Keep the base in Spanish and add `values-fr/`, `values-de/`, … | Fixes the symptom for the languages you ship | Every *other* device still shows Spanish; contradicts the repo's own English-by-default convention (`openspec/config.yaml`) — **rejected** |
| Swap the file contents in place (English into `values/`, Spanish left in `values-en/`) | Smallest textual edit | Keeps Spanish under a folder named `en`, so the file lies about its language — **rejected** |

## Impact

- `app/src/main/res/values/strings.xml` (rewritten in English), `app/src/main/res/values-es/strings.xml` (new), `app/src/main/res/values-en/strings.xml` (deleted).
- `app/src/test/java/com/foco/app/structure/DefaultLocaleResourcesTest.kt` (new JVM tripwire, file-scanning style like `LayerRulesTest`).
- **No dependency** is declared: nothing active claims `res/values*/`. The new test file sits inside `app/src/test/**`, a glob that three active drafts claim; today that is a glob-level warning (no shared file), and the plan if one of them starts applying is to sequence then — the same call recorded for `layer-check-scope` in `DECISIONS.md` (2026-10-18: a small fix is not queued behind drafts).
- Out of scope: the Spanish **prose** (comments/KDoc/test messages) — that is the separate draft `english-source-prose`.
