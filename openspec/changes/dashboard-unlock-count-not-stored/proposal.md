# Proposal — dashboard-unlock-count-not-stored

## Why

The dashboard collapses "no count stored" into a counted zero:
`val unlocks = readOr(0) { store.unlocksFor(date) ?: 0 }` (`DashboardScreen.kt:220`, rendered
around `:323-327`). The store and the database go out of their way to keep the distinction —
"`null` means no count was stored, `0` is a real answer" (`SessionHistoryStore.kt:56-57`,
`FocoDatabase.kt:51-54`) — and the day's marker was added at the v2 bump, so **days consolidated
before the bump carry no count**. The UI throws the distinction away and asserts "0 desbloqueos"
for a number that was never measured.

Contract in force:

- `openspec/specs/usage-tracking/spec.md:345-347` — scenario "The upgrade keeps what was stored":
  after the v2 upgrade "the day's count reads as **not stored**".
- `usage-tracking/spec.md:350` — the week-view rule the dashboard mirrors: "a day without stored
  data SHALL read as absent, not as zero".
- `DECISIONS.md:353` — "a reader distinguishes 'not stored' from `0`".

## What changes

- The unlock line renders the count only when a count is stored; for a not-stored day it reads
  absent (no zero claim), the same way the week bar reads absent.
- Regression: `DashboardStateTest` pins absent ≠ 0 for a stored day without a count;
  `UnlockCountTest` keeps the real-count rendering honest on device.

## Risks

- Purely presentational; no data or schema change.
- The exact absent rendering (line hidden vs `—`) is an implementation choice inside the
  requirement's wording ("absent, not as zero"); the test pins the absence of a zero claim, not a
  pixel.

## Spec delta

`specs/usage-tracking/spec.md` MODIFIES *A finished day keeps its unlock count* — restated verbatim plus the scenario *A day without a count reads as absent, not zero*, extending the store's not-stored answer to the reader the defect hit.

## Origin

Filed 2026-10-21 by the anchored bug-finder audit (finding 11, severity low, 🔴 regression of the
v2 unlock-count work); `kind: fix` per /spec-triage. Sequenced after the parallel
`dashboard-periodic-refresh-resilience` (same file). Awaiting human acceptance — no code written.
