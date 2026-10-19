# Proposal — Comments say the why in at most four lines

## Why

The constitution already asks for it: `AGENTS.md:32` — *"Files that touch the OS, database, storage or network start with a short header: which physical resource, where it lives, its lifecycle. **No other noise comments.**"* No living requirement enforces it, so the tree drifted: under `app/src/**` there are **30 comment blocks of 5+ lines**, **7 of 8+** and one of **16** (`androidTest/java/com/foco/app/ui/SettingsScreenTest.kt:33`, the activity-recreation stability notes).

Long comments are the ones that rot first. They restate what the code says, they name behaviours and line numbers that move, and they bury the one thing that must stay in place — the physical resource and its lifecycle. This repo already keeps explanations where they belong: mechanism notes in `docs/mechanisms/` (anchored by `/spec-explain`), decisions in `DECISIONS.md`, and the story of each change in its `walkthrough.md`. The code only needs to say where it stands and point at the rest.

## What changes

- A comment block (KDoc, `//`, `/* … */`, `<!-- -->`) under `app/src/**` is **at most 4 lines** — the mandated physical-resource header fits inside that budget.
- An explanation worth keeping (a *why*, a debugging note, a mechanism) **moves** to `docs/mechanisms/` — the same home `/spec-explain` uses, one note exists today (`ui-flow.md`) — or to the change's `walkthrough.md` / an ADR, and the code keeps a **one-line pointer** where it was.
- A JVM tripwire (`ShortCommentsTest`) makes the budget machine-checked: it walks the comment blocks under `app/src/**` and fails naming `file:line`, the block's length and its first line. **It is red on today's tree** (30 blocks) — that red is this refactor's regression evidence.

No behavior changes: only comment text moves or shrinks. No code line, no string value, no resource key.

## Proof (a refactor changes no behavior)

- The budget's own oracle: `ShortCommentsTest` red before, green after.
- Behavior untouched — the suite is the proof, as the repo defines for a refactor (`structure-housekeeping` did the same): `LayerRulesTest` (tree and layers), `SessionBuilderTest` (domain results), `SettingsScreenTest` (rendered UI, including the Spanish path). The moved notes are *about* `SettingsScreenTest`, so running it on the AVD after the move is the check that nothing was lost.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **4-line budget + move the keepers to `docs/mechanisms/` + a JVM tripwire** | Enforces `AGENTS.md:32`; keeps the knowledge that is worth keeping, in the place the repo already uses; machine-checked red-before/green-after | **Chosen (human, 2026-10-18)** |
| 6-line budget | Leaves the mandated headers alone (they are 3–5 lines) | Leaves every 8+ block standing — the essays are exactly what "explica demasiado" means — **not chosen** |
| Editorial judgement, no tripwire | No false positives | Nothing enforces it, so the tree drifts back; and a `refactor` would have no regression test — **rejected** |
| Delete the long comments without moving anything | Smallest diff, fastest | Loses knowledge that lives nowhere else — e.g. why the appearance test must re-open Settings after a pick, which came from a red suite on the AVD — **rejected** |
| Reuse `docs/mechanisms/` as-is, no new note | Option 0: the contract already exists (`ui-flow.md`) | Only partly: the data path (day window, `KEYGUARD_HIDDEN`, the session fold) has no note yet, so one more arrives — **partially reused** |

## Impact

- Comments under `app/src/**` (Kotlin, `AndroidManifest.xml`, `res/values/themes.xml`, `res/xml/locales_config.xml`); new `app/src/test/java/com/foco/app/structure/ShortCommentsTest.kt`; new/updated notes under `docs/mechanisms/`. `res/values*/strings.xml` is deliberately **not** a target: its comments are single section headers, inside the budget, and those files belong to `english-default-resources`.
- `deps` declared, deliberately: `english-default-resources` (its files sit inside `app/src/**`, so they are settled first) and the four active changes that claim the same globs (`cross-midnight-sessions`, `same-millisecond-transitions`, `layer-check-scope`, `back-arrow-inset`). The effect is the order — this sweep goes **last** among them, which is also the conflict-free order; the tripwire is what makes re-applying it after their merge cheap.
- **Order with the sibling draft**: the human chose *trim first, translate after* (2026-10-18), so `english-source-prose` now declares a dep on this change — it translates what survives, not what will be deleted.
- Not a `tech_brief`: no state, no concurrency, no network, no platform API, no dependency.
- Out of scope: `scripts/gates/**` (its canonical copy lives in the `spec-growth` skill), the repo-root build files, `gradle/**`, `ARCHITECTURE.dsl` and `app/proguard-rules.pro`. The prose *inside* `docs/` is not trimmed — notes only arrive there.
