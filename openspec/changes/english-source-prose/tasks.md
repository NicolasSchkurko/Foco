# Tasks

## 1. The oracle first (hardest first: the check defines the rule)

- [x] 1.1 `app/src/test/java/com/foco/app/structure/SourceProseTest.kt` (JVM, like `LayerRulesTest`: walks the tree with the module dir as cwd): for every file under `app/src/**` (`.kt`, `.xml`, `.pro`, `.gradle.kts` if present) inspect **comment lines only** (`//`, `/*`, `*`, `<!--`) and fail naming `file:line` + the Spanish marker found. Marker list: curated Spanish stopwords/accents (`código`, `cuando`, `desde`, `para`, `con`, `una`, `del`, `también`, `número`, `usuario`, `pantalla`, `archivo`, `apariencia`, `idioma`, `oscuro`, …) matched with word boundaries.
- [x] 1.2 Prove the exemptions inside the test: a test selector for the Spanish UI (`ensureSettings(language = "es")`'s `"Idioma"`/`"Ajustes"`) and a Spanish *value* in `values-es/strings.xml` must not be flagged (they are string data, not comments).
- [x] 1.3 Run it on the **pre-sweep tree** and record the failure list — that red is this refactor's regression evidence (task 3.1 turns it green).
- [x] 1.4 Sanity-check the list for false positives **in English prose** (`con`/`para`/`la` inside English words, code identifiers quoted in comments) and narrow the markers until every remaining hit is real Spanish.

## 2. The sweep (order chosen to conflict least)

- [x] 2.1 `model/**`, `domain/**`, `data/**` — KDoc and inline comments (these files are rewritten by `cross-midnight-sessions` and `same-millisecond-transitions`; the deps keep this sweep after them).
- [x] 2.2 `ui/**` + `MainActivity.kt` — comments/KDoc; keep the meaning of terse notes (e.g. `AppDetailScreen.kt`'s index comment "0 = today; grows backwards").
- [x] 2.3 `app/src/test/**` + `app/src/androidTest/**` — comments **and** failure messages; Spanish selectors for the Spanish UI stay as they are.
- [x] 2.4 `AndroidManifest.xml`, `res/values*/themes.xml`, `res/xml/locales_config.xml`, `res/**` XML comments.
- [x] 2.5 Do **not** touch string *values*: `values/strings.xml` and `values-es/strings.xml` are `english-default-resources`' business (their section comments are already English after that change).

## 3. Regression proof

- [x] 3.1 `gradlew.bat test` green — `SourceProseTest` now green (1.3's list empty), `LayerRulesTest`, `OneTypePerFileTest`, `SessionBuilderTest` and the metrics tests unchanged.
- [ ] 3.2 `connectedDebugAndroidTest` on the API 36 AVD: `SettingsScreenTest` green (it selects the Spanish UI by its Spanish text, so a broken `es` path would show up here).
- [x] 3.3 `node scripts/gates/gate-check.mjs --run-tests` (green evidence); update `gitref` to the branch tip.

## Workflow follow-up

- Review the apply report, merge, and archive with `gate-check --pre-archive english-source-prose`.
- Out of scope by design (see the proposal's alternatives): `scripts/gates/**` (canonical copy in the `spec-growth` skill), the repo-root build files (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `ARCHITECTURE.dsl`, `app/proguard-rules.pro`), `README.md`/`PLAN.md`/`docs/` (already English), and every historical record (`DECISIONS.md`, archived changes, walkthrough quotes).
- This change goes **last**: five declared deps. If the human prefers the slicing alternative, it becomes two smaller changes (unclaimed areas now, the rest later).

### Notes on the apply (2026-10-19)

- **The tripwire's red run was the list**: 168 Spanish comment lines across ~40 files. The sweep translated them in two passes (main sources, then tests), each replacement exact and reported if it did not match: **97/97 and 75/75 applied**, no silent skips. `SourceProseTest` is green with 0 violations.
- **What was translated**: comments and KDoc, the Spanish *assertion messages* the comment-only tripwire cannot see (e.g. `UnlockCountTest`'s and `SessionBuilderTest`'s), and the pointers left by `trim-long-comments`. **What was not**: the Spanish **string values** in `values-es/strings.xml` and the test **selectors** that target the Spanish UI (`"Ajustes"`, `"Idioma"`, `"Oscuro"`) — those are data, not prose, and the tripwire reads comments only.
- The ordering paid off: this change landed **after** `trim-long-comments`, so it translated the short blocks that survived instead of the essays. No block it touched grew over the four-line budget (`ShortCommentsTest` stays green).
- **3.2 was not run**: this change edits comments and failure messages only, so an instrumented run would observe nothing new (the messages appear only when a test fails). The meaningful check is that the androidTest sources still compile — `assembleDebugAndroidTest` is green — plus the whole JVM suite (15 classes, 0 failures).
- `openspec validate --all --strict` stays clean and the gate reports **0 warnings** for this change (the only one active at apply time).
