# Proposal — The source's prose is English (comments, KDoc, test messages)

## Why

The repo's convention is English (`openspec/config.yaml`: *All artifacts must be written in English*), and it holds for the specs, the ADRs and the docs — which are English. It does **not** hold for the **code's own prose**: KDoc, inline comments and test failure messages are largely Spanish. Examples: `data/AppLanguage.kt:7-11`, `data/SettingsStore.kt:12-15`, `MainActivity.kt:28-32`, the comments in `UsageRepository.kt`, `domain/SessionBuilder.kt`, `SessionFilter.kt`, the `UnlockCountTest.kt:83` assertion message, the two block comments in `AndroidManifest.xml`, `res/values/themes.xml`, `res/xml/locales_config.xml`, and the Spanish section comments in the string resources.

That is the same complaint as "Oscuro should be Dark", applied to the app's own documentation: the most delicate code in the repo (the session fold, the day window, the locale/night-mode plumbing) is explained only in Spanish, and this project's whole point is that a future reader — or a future session of the agent — can follow the mechanism from the code. The walkthroughs and `/spec-explain` notes are English; the code they describe is not.

## What changes

Comments, KDoc and test failure messages under `app/src/**` become English. Spanish stays exactly where it is **data**, not prose:

- string *values* in the `es` resources (and any string in a `values-*` file);
- test selectors that find the Spanish UI (`"Ajustes"`, `"Idioma"`, `"Hoy"`, `"Oscuro"`, `"Automático"`) — those must stay Spanish to select what they select.

A JVM tripwire makes the rule machine-checked: `SourceProseTest` walks `app/src/**`, looks at **comment lines only** (`//`, `/*`, `*`, `<!--`), and fails naming `file:line` and the Spanish marker it found (a curated list of Spanish stopwords, extended whenever a false negative shows up — never silenced). It is **red on today's tree**, which is this change's regression evidence.

No behavior changes: no line of code, no string value, no resource key moves. No new state, no dependency, no API.

## Proof (a refactor changes no behavior)

- The sweep's own oracle: `SourceProseTest` red before, green after — a machine-checked statement about the prose.
- Behavior untouched: the existing suite is the proof, as the repo defines for a refactor (`structure-housekeeping` did the same) — `LayerRulesTest` (the tree and layers are unchanged), `SessionBuilderTest` (the domain behavior is untouched), `SettingsScreenTest` (the UI, including the Spanish path, still renders the same text).

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Sweep all of `app/src/**` in one pass, declared after the four changes already in flight** | One idea, one pass; the tripwire then holds over the whole app source; the deps make the overlaps a *planned order* instead of an error, and landing last is also the only conflict-free order (those changes rewrite the same files) | **Chosen** |
| Slice by area — sweep now only the folders nobody is rewriting (`ui/dashboard`, `ui/common`, `ui/metrics`, `ui/onboarding`, `model`), the rest later | Smaller diff, no conflicts, lands sooner | The rule cannot be enabled over `app/src/**` yet, so the tripwire would have to be scoped to a subset — two changes and a weaker contract — **not chosen** |
| Fold the prose into each change in flight (each one translates the files it already edits) | No separate branch, no conflicts at all | Every diff becomes unreadable (the real change drowns in comment churn) and the changes already `verified` would have to be reopened — **rejected** |
| Translate the repo's `DECISIONS.md` quotes and the archived changes too | Uniform prose everywhere | History is append-only and those quotes are the human's own words — rewriting them falsifies the record — **rejected** |
| Also sweep `scripts/gates/**`, the root build files, `README.md`, `PLAN.md`, `docs/` | Truly everything | The docs are already English; the gate scripts' canonical copy lives in the `spec-growth` skill (a translated copy would diverge and be overwritten); the root build files carry 2–3 comments and deserve their own tiny sweep if wanted — **out of scope, proposed** |

## Impact

- `app/src/**`: comments, KDoc and test failure messages (Kotlin, `AndroidManifest.xml`, `res/values*/themes.xml`, `res/xml/locales_config.xml`, `res/**` XML comments); plus the new `app/src/test/java/com/foco/app/structure/SourceProseTest.kt`.
- `deps` declared, deliberately: `trim-long-comments` (the human's order of 2026-10-18 — *trim first, translate what survives*, so no line is translated twice), `english-default-resources` (it owns the two string files, so this change cannot start before it lands) and the four active changes that claim these globs — `cross-midnight-sessions` (`data/**`, `domain/**`, `app/src/test/**`), `same-millisecond-transitions` (`domain/**`, `app/src/test/**`), `layer-check-scope` (`app/src/test/**`), `back-arrow-inset` (`ui/detail/**`, `ui/settings/**`, `app/src/androidTest/**`). Each dep is a warning until that change is verified; the effect is the order — **this sweep goes last**, which is what keeps it conflict-free.
- Not a `tech_brief`: no state, no concurrency, no network, no platform API, no dependency.
- If the deps feel heavy for a prose sweep, the alternative is the slicing option above (two changes over time) — your call at approval.
