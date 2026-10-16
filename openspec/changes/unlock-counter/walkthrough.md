# Walkthrough — unlock-counter (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them.

## Mechanism

The day's usage data arrives through one system query: `UsageStatsManager.queryEvents(startOfDay,
end)`. It carries many event kinds; we already consume foreground events (RESUMED/PAUSED/STOPPED)
and now also **`KEYGUARD_HIDDEN`** — the platform event fired when the keyguard is dismissed
(phone unlocked). The repository maps each `KEYGUARD_HIDDEN` timestamp into the day result, which
becomes a small primitive pair: `DayData(sessions, unlockTimes)`. The dashboard derives the count
the same way it derives every metric — in the UI, from plain data (`unlockTimes.size`) — and
renders it in the existing subtitle: "Uso en pantalla: X · N desbloqueos". Because the count comes
from the same load, it refreshes with the same triggers (first show, every return to foreground,
the 30-second periodic loop) and never runs extra background work.

## Physical traceability

- `app/src/main/java/com/foco/app/model/day/DayData.kt` — pure data (no Android imports); the day
  primitive.
- `app/src/main/java/com/foco/app/data/UsageRepository.kt` — the only file touching the platform
  query; maps `KEYGUARD_HIDDEN` → `unlockTimes`.
- `app/src/main/java/com/foco/app/ui/dashboard/DashboardScreen.kt` — renders the count in the
  subtitle.
- `app/src/androidTest/java/com/foco/app/ui/UnlockCountTest.kt` — E2E; generates real unlocks via
  shell (`SLEEP` → `WAKEUP` → dismiss) and reads the subtitle from the running app.

## Limits and failure modes

- **Retention**: `queryEvents` only serves the last few days — the count is bounded by the same
  window as the sessions (today always fits).
- **No secure lock**: on a device/emulator with no lock set, each screen wake can fire
  `KEYGUARD_HIDDEN`; the count then reads as "times the screen was picked up". This is the
  platform's standard semantics, fixed in the spec.
- **ADB-simulated unlocks**: `KEYCODE_SLEEP`/`WAKEUP`/`82` reproduce the sequence; if the emulator
  has no lockscreen configured, `KEYGUARD_*` events may not fire — the test then fails
  informatively (enable the swipe lock).
- The count is **not** per app and does not try to detect "cheating" (e.g., unlocking and
  immediately locking). It is a faithful count of keyguard dismissals.

## Touched APIs

- `UsageEvents.Event.KEYGUARD_HIDDEN` (platform; same query as the sessions).
- `UsageRepository` contract (return type grows to `DayData`) — internal callers only.
- No new dependencies; no new permissions.

## Implicit decisions

- **Standard definition** (keyguard dismissed = unlock), decided with the human; the alternative
  "strict pairs SHOWN→HIDDEN" was rejected (same numbers in practice, more state).
- **Primitive, not derived data**: the repository returns timestamps; the UI counts. Consistent
  with the sessions philosophy.
- **Pre-declared ordering** (A+ policy): after `detail-back-ux`; `app-settings` waits on it.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. Why does the unlock count not need any new permission or dependency? — TODO(human)
2. On a phone with no lockscreen, what does the number actually count, and why is that considered
   standard? — TODO(human)
3. If tomorrow the count shows 3× yesterday's usual numbers, what are the first two things you
   would check? — TODO(human)

When satisfied, the sign-off is recorded in `/spec-verify` (`understood_by` + `understood_at` in
`.openspec.yaml`).
