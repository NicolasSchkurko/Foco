# Proposal — post-block-recheck-undelivered-resume

## Why

Every re-block path in force is event-driven or periodic: the accessibility event
(`UsageMonitorAccessibilityService.kt:62-68`) and the 2-minute tick. On 2026-10-21 a real user hit
the hole between them: after a block finished and sent the app home (`finishBlock`, `:159-164`), the
user reopened the blocked app and used it **without any card**, and the on-device log of that window
proves no `TYPE_WINDOW_STATE_CHANGED` was delivered at all — not for the reopen, not even for the
recents screen (`foco_diag` capture; the neighbouring lines survived, so nothing was lost and
`onAccessibilityEvent` logs every subscribed event it receives). The platform simply never told the
service the app returned, so no code path could react.

The case did not reproduce in 15+ controlled and manual attempts (icon, recents card, bottom-edge
quick-switch, launcher search), so it is a dispatch race at the transition right after our own HOME
+ overlay dismissal, not a defect in the event handling. Waiting for the 2-minute tick is the
documented residual (`DECISIONS.md:439-444`); it leaves the user inside the blocked app for up to
2 minutes, which is exactly the reported experience.

Contract already in force (and honestly amended here, not bypassed):

- `openspec/specs/app-limits/spec.md:37-42` — "The monitor checks today's usage at most every two
  minutes" enumerates the triggers: "at most every 2 minutes and on every accessibility foreground
  event". This change adds one more trigger, one-shot and only right after Foco's own block.
- `openspec/specs/app-limits/spec.md:74-79` — the block is the three-second limit screen that ends
  in HOME; the re-check runs only after that, and `evaluate()` already refuses to start while the
  overlay shows (`:97-100`).
- `DECISIONS.md:418-423` — the 2-minute periodic cadence itself is NOT changed: the new trigger is a
  one-shot after Foco's own block, not a periodic poll. This reverses the archived non-goal of
  `duplicate-block-after-home` ("this change does not alter that cadence"), so it is offered to the
  human as an explicit contract decision, not as silence.

## What changes

- `finishBlock()` arms a single coroutine (the `armBoundary` shape, `:127-142`) that re-checks at
  ~+2 s and ~+6 s: if the blocked package is the foreground package again **according to today's
  UsageStats sessions** (the same source as the monitor), it re-enters `evaluate(packageName)`,
  which re-decides the reason and shows the card. No accessibility event is required.
- The decision is a pure, JVM-testable predicate next to the existing session arithmetic:
  `AppLimitRules.isForegroundAt(sessions, packageName, at)` — a session of that package covering
  `at`. A reopen merged into the previous session by `mergeContiguousSessions` still covers `at`,
  so "returned in the same minute" is never missed; a session that ended before `at` yields false,
  so nothing is blocked when the app stayed home.
- `UsageMonitorEvaluator` grows the sibling read (`isForegroundNow`) reusing `UsageRepository` —
  one adapter, no direct repository call from the service.
- The pending re-check job is cancelled on `onDestroy`; the existing `blockHost.isShowing` guard
  inside `evaluate` covers a re-check that lands while a countdown is already running, so a normal
  event-driven reopen never produces a second card.
- Regression test: `AppLimitRulesTest` pins the predicate on the exact reported shape — the blocked
  app's session covering the re-check instant (including a session whose start precedes the block,
  i.e. the merged reopen) → re-check must fire; session ended at HOME → no re-check. The service
  glue (the two delays) stays untested exactly like `armBoundary`, the accepted precedent.

## Alternatives considered

1. Subscribe to more accessibility event types (`typeWindowsChanged`) — tried live during
   diagnosis: it never fired, because the gap is "no window event at all". Rejected.
2. Poll the foreground periodically from usage stats — changes the 2-minute cadence contract and
   runs forever; the one-shot after our own block is strictly smaller. Rejected.
3. Do nothing until the monitor tick — the documented residual, and the user experience that was
   reported. Rejected by the human.

## Risks

- UsageStats event lag: if the app left again microseconds before the re-check, the card could show
  over the launcher. Bounded: HOME was just sent by us, sessions are live on the device, and the
  next window event re-syncs the tracked foreground.
- Loop potential: each re-check requires a user reopen to have happened; without user action the
  job is a no-op. A user who keeps reopening gets a card each time — the intended behaviour.
- File overlap with two drafts from the 2026-10-21 bug-finder audit:
  `monitor-revalidate-foreground-before-block` and `monitor-input-method-not-foreground` also target
  `UsageMonitorAccessibilityService.kt` but edit `evaluate`/`requestBlock`; this change touches
  `finishBlock` and one new private job. Different hunks; `launchable-packages-refresh` touches
  `UsageRepository.kt`, which this change only reads.

## Origin

Filed 2026-10-21 by /spec-triage from a live user report ("reabrí desde pestañas y no salió la
tarjeta"), with on-device evidence of the undelivered resume. `kind: fix` per the flow. Awaiting
human acceptance — no code written.
