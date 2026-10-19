# Mechanism note — Why the instrumented tests have to re-open screens and tolerate gaps

> Anchored by the `trim-long-comments` change (2026-10-19). The contract lives in `openspec/specs/`
> (`spec-tooling`, `app-settings`, `usage-tracking`); code is the authority — refs are `file:line` at the time of writing.

## Why `ensureSettings` is called again after a pick

- Applying a **language** re-creates the activity: appcompat's `setApplicationLocales` is a configuration
  change, and the screen can come back at the start. `ensureSettings` (`SettingsScreenTest.kt`) re-opens
  Settings when the language section is not on screen.
- Applying an **appearance** can also re-create the activity (the appcompat `uiMode` change), so the same
  helper is called again after that pick. The re-creation is observable in the test and is the reason the
  assertion waits for the hierarchy to settle (`selectedRow`).
- During a re-creation there are instants **with no Compose hierarchy**. Checks go through `showsText` /
  `showsDescription`, which treat that gap as "not yet" instead of failing, and through `waitUntil` where
  the value is read from a global (the appcompat night mode).

## Why "restarts" are `close()` + a new launch

Chaining `recreate()` on top of another re-creation leaves `ActivityScenario`'s tracking hanging, so a
cold restart is spelled as `scenario.close()` followed by `ActivityScenario.launch(...)`.

## Where the assertions stop

The window backdrop is not asserted (no public getter on `Window`); the positional assertion for the back
control measures the semantic node in dp with a threshold that has margin on both sides (`≤ 16 dp`,
measured ~8.4 dp after the fix and ~24.4 dp before it).
