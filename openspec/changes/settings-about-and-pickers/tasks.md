# Tasks — settings-about-and-pickers

Order: the selector swap first (it is the only part that breaks green tests), then the identity
block, then the license, parity and gate passes. Apply after `weekly-json-export` (declared dep):
both touch the two `strings.xml` files.

## 1. Selectors become one row + dropdown (riskiest)

- [x] In `ui/settings/SettingsScreen.kt`, replace `OptionRow` with a row that shows `SectionTitle` + the active label and a trailing chevron; tapping it opens the dropdown.
- [x] Add the anchored `DropdownMenu` (`expanded` + `onDismissRequest`, anchored to the row) listing `LanguageOption.entries` / `AppearanceOption.entries` with the active one marked; picking calls the existing `store.setLanguage` + `AppLanguage.applyToSystem` / `store.setAppearance` and closes the menu.
- [x] Keep `languageLabel` / `appearanceLabel` (`SettingsScreen.kt:168-179`) as the single source of labels for both the row and the menu.
- [x] Rewrite the language and appearance tests in `app/src/androidTest/java/com/foco/app/ui/SettingsScreenTest.kt`: drop `assertIsSelected` and the index-based `onAllNodesWithText("Automatic")[1]`, open the dropdown, pick the option, assert the closed row reads the chosen label.
- [x] Update the screen's file doc comment (`SettingsScreen.kt:42-45`) to describe the dropdowns instead of the radio rows.
- [x] `gradlew.bat connectedDebugAndroidTest --tests com.foco.app.ui.SettingsScreenTest` green on the emulator.

## 2. Identity block above Language

- [x] Add `data/AppIdentity.kt`: the repository URL constant, `versionName(context)` (installed package, nullable), and `repositoryIntent()` returning `Intent(ACTION_VIEW, uri)` — the `data/UsageAccess.kt:25` shape.
- [x] Render the block as the first `item` of the `LazyColumn` (`SettingsScreen.kt:79`), above the Language item: three centered lines, version from `AppIdentity`, link row calls `context.startActivity(AppIdentity.repositoryIntent())`.
- [x] Add the strings in `values/strings.xml` and `values-es/strings.xml` (credit + link label + version format); keep the base English and the proper noun unchanged.
- [x] Extend `SettingsScreenTest.kt`: the block is present above the `Language` title, its lines are centered, the version line matches the installed `versionName`, and the Spanish locale shows the `es` labels.
- [x] Add an instrumented assertion on the adapter itself (`AppIdentity.repositoryIntent()`: `ACTION_VIEW` + the exact `https` URL) — no `espresso-intents` dependency needed.
- [ ] Tap the link once on the emulator and record the evidence: the browser opens the repository, Foco stays in the background.

## 3. MIT license

- [x] `LICENSE` at the repo root with the MIT text and `Copyright (c) 2026 Nicolas Schkurko`; confirm the year and the holder name with the human before merging.
- [x] Check that `README.md` does not contradict it (a "no license" note, if any, is corrected in the same commit).

## 4. Verification

- [x] `gradlew.bat test` green (JVM: `DefaultLocaleResourcesTest` parity, `AppAppearanceTest`).
- [x] `openspec validate settings-about-and-pickers --strict` clean.
- [x] `node scripts/gates/gate-check.mjs` shows no new error beyond the declared open decision.
- [x] `node scripts/gates/build-board.mjs` regenerates `BOARD.md`.
