# Proposal — startup-history-gate

## Why

The first open mounts the home while `MainActivity` starts the history backfill on IO. The week and
detail read the store before the six finished days are consolidated, so they look incomplete until
the process is restarted and the next backfill has already written them. The request is to load the
seven-day history on the first open, even if that means a blocking loading screen.

## What

- Move the startup backfill behind the usage-access gate: it runs when the permission is available,
  not before it.
- Add a startup gate in `App`: after permission, show a loading state until the shared
  `ConsolidationRunner` run ends; only then mount `FocoNavHost`.
- On failure, do not trap the app: leave the loading state, log/propagate the failure to the existing
  runner outcome, and let the next start retry.
- Keep the weekly/detail reads as they are: once the gate opens, `WeekContent` reads the populated
  store and the detail's `storedDaysFlow` already follows it.

## Expected outcome

On the first open after permission, the user sees a loading screen and then a week/detail with the
same seven-day history that used to appear only after closing and reopening. Granting permission from
onboarding produces the same flow without a process restart; a failed backfill still leaves a usable
app and retries next start.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| Block the home on the startup run (chosen) | Guarantees the week/detail are complete on first open; simple; one gate | The first open waits for up to six days of consolidation |
| Keep the home immediate and make `WeekContent` reactive to the store | No blocking screen | The user explicitly asked for a loading screen; the week could still flash empty |
| Block and also make the week reactive | Covers a later nightly update while open | More code than the first-open problem needs |
| Re-read the week on entry/resume | Small UI change | Races the backfill; does not guarantee the first open |

## State and persistence mechanism

No new store or schema. The gate uses the existing `ConsolidationRunner` over `SessionHistoryStore`
and `foco.db`; the loading state is in memory in `App`. The nightly scheduler stays unchanged. No
network, no new permission.

## Prior decisions

This is the upgrade trigger recorded by the archived week-screen shortcut (“one store read per screen
entry, no resume re-read”); the detail already solved the same class of problem with
`storedDaysFlow`. The backfill remains automatic: no manual sync control is added
(`DECISIONS.md:321-326`).
