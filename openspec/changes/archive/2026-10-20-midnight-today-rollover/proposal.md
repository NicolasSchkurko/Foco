# Proposal — midnight-today-rollover

## Why

`today` is captured once with `remember { LocalDate.now() }` in the home, the detail and the week,
and `WeekExportService` takes a `today` parameter while reading today's sessions with
`repository.loadToday()`. If the app is open or backgrounded across midnight, the live read returns
the new day but the title, ring, week window and export still use the old date. “Today live” becomes
the new day's data labeled with the previous day.

## What

Use one current-day source that updates when the app is resumed and when the date changes, and pass
it to the home, detail, week and export. The export SHALL read the day's live data with
`repository.loadDay(today)`, not `loadToday()`, so `weekEnd` and the live entry always agree.

No persistence, no schema change.

## Expected outcome

After midnight (or on the next resume), the home's “Today”, ring, week window and detail selector use
the new date; the export's `weekEnd` and live sessions are the same day; the previous day is read from
history instead of being mixed with today.

## State and persistence mechanism

No new store. The current date is in-memory state derived from the device clock; the existing
`foco.db` history and `UsageRepository` remain the sources. The state lives while the Activity lives
and is refreshed on resume; no backup or network change.

## Alternatives considered

| Option | Why yes | Why not |
| --- | --- | --- |
| One current-day state refreshed on resume/date change (chosen) | Covers every screen; one source | Needs a small clock seam for tests |
| Recompute `LocalDate.now()` in each screen | No shared state | The screens can still disagree at the boundary; hard to test |
| A ticking clock every minute | Immediate | Wakes the app for a rare boundary; unnecessary for a resume-driven UI |
