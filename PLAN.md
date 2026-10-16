# PLAN — Foco (tracker + limits + Reels blocking)

Phased implementation plan. Each phase ends with something usable and a concrete
"done" criterion. The goal is to learn Android by building, not the other way around.

## Core idea

Beating infinite scroll is not done with walls (people bypass them), but with
**friction**: every decision to open the app costs a few seconds and requires
intention. The tracker is the mirror; friction is the tool.

## Design principles

1. **Friction before walls.** Waits, breathing pauses, "write why" — change the
   decision, do not declare war on the user.
2. **Everything local.** No accounts or servers: usage data stays on the phone.
3. **The platform rules.** Android has no API to "block part of another app".
   Everything Reels-related is built by reading the UI, it is heuristic, and it
   needs maintenance. The design assumes that (rules engine, not hardcoded logic).
4. **Usable phases.** No three months without trying anything on the phone.

## Target architecture (where everything goes)

| Component | Responsibility | Phase |
|---|---|---|
| Compose UI (`ui/`) | Dashboard, limits, settings | 1+ |
| `UsageRepository` (`data/`) | Read UsageStatsManager → sessions | 1 ✅ |
| Room (`data/db/`) | Own history, budgets, rules | 2 |
| `UsageMonitorService` (FGS) | Track ongoing usage, trigger limits | 3 |
| `FrictionOverlay` | Friction screen over the target app | 3 |
| `LimitEngine` | Per-app/hour budgets + notifications | 3 |
| `ReelsBlockerService` (Accessibility) | Detect Reels/Shorts and act | 4 |
| `rules.json` (`assets/`) | Selectors and actions per app/version | 4 |

---

## Phase 0 — Environment

- [ ] Install Android Studio; open `Foco`; first build on the phone.
- [ ] USB debugging on the phone; accept the prompt from the PC.
- [ ] (For Phase 3) Disable battery optimization for Foco.

**Done when:** the dashboard runs on your phone and you see your real usage.

## Phase 1 — Day tracker ✅ (scaffold included)

What's there: permission onboarding, day total, hourly activity (24 bars),
app ranking with icon, duration and time of the last use.

Key files:

- `MainActivity.kt` — decides onboarding ↔ dashboard and re-checks the permission on `ON_RESUME`.
- `UsageAccess.kt` — the `PACKAGE_USAGE_STATS` permission is checked as an AppOp.
- `data/UsageRepository.kt` — events → sessions → ranking + hourly buckets.
- `ui/DashboardScreen.kt` — the whole dashboard.

Natural extensions (whenever you feel like playing): date picker, comparison
with yesterday, CSV export, "today" widget.

**Done when:** ✅ already working.

## Phase 2 — History and persistence (Room)

Goal: stop depending on what the system retains (limited) and get a base for limits.

- [ ] Entities: `SessionRecord` (pkg, start, end) and `DailyAggregate` (pkg, day, total).
- [ ] `WorkManager`: nightly job that consolidates the day and cleans up old raw data.
- [ ] **Week** screen: bars per day + totals per app.
- [ ] Schema migration from day one (good habit).

New concepts: Room (DAO + Flow), WorkManager, migrations.

**Done when:** 7+ days of history visible, survives reboots.

## Phase 3 — Limits, reminders and friction

Goal: per-app budgets and real friction when going over. **This is where the
foreground service appears.**

- [ ] Per-app budget (e.g. Instagram 30 min/day), configurable in Settings.
- [ ] `UsageMonitorService` (foreground, `specialUse` type — mandatory since Android 14):
      tick every 15-30 s with UsageStats + accessibility for real time.
      Lightweight alternative: `WorkManager` every 15 min (enough for limits, not for instantaneity).
- [ ] Notifications: own channel + `POST_NOTIFICATIONS` permission (Android 13+).
- [ ] **Friction overlay** (`SYSTEM_ALERT_WINDOW` + `TYPE_APPLICATION_OVERLAY`):
      full-screen over the target app with:
        - 10 s countdown before enabling "Open anyway",
        - "write why you're opening it" option,
        - "5 more minutes" exception mode with a cost (wait + record).
- [ ] UX reference patterns: **one sec**, **ScreenZen**.

**Done when:** if you go over the Instagram limit, the phone really stops you
and giving in costs (a little).

## Phase 4 — Reels / Shorts blocking (the artisanal part)

Goal: opening Reels is not an option. There is no API: the UI is read with an
**AccessibilityService**.

- [ ] `ReelsBlockerService` with `accessibility-service` config XML.
- [ ] Detection: `onAccessibilityEvent` (`TYPE_WINDOW_STATE_CHANGED` /
      `TYPE_WINDOW_CONTENT_CHANGED`) → bounded BFS over `rootInActiveWindow`,
      matching by `viewIdResourceName` (regex), text and `contentDescription`.
- [ ] **Rules engine** in `assets/rules.json` (so you don't recompile for every Instagram change):
      ```json
      {
        "package": "com.instagram.android",
        "match": {
          "viewIdRegex": ".*clips.*",
          "textAny": ["Reels"],
          "descAny": ["Reels", "Clips"]
        },
        "action": "back"
      }
      ```
- [ ] Actions: `BACK` and, if still blocked after ~300 ms, `HOME`.
- [ ] Anti false positives: whitelist (story viewer, DMs), 1 s debounce,
      decision recorded in a local log (last N).
- [ ] Anti-impulse: disabling the service requires a PIN.
- [ ] Sideloaded (Android 13+): a manually installed app needs
      "Allow restricted settings" to be able to enable Accessibility.
- [ ] Test with logcat + a real test per app (Instagram, TikTok, YouTube).

Open source references: `duartebarbosadev/Scrolless`,
`JacobX0126/shorts-blocker-android` (the latter documents exactly these
decisions, including the BACK→HOME escalation and the PIN).

**Done when:** opening Reels sends you back to the feed consistently, without
breaking the rest of the app (chats, stories, profile).

## Phase 5 — Hardening (optional)

- [ ] **Device owner** (once, via adb: `adb shell dpm set-device-owner`):
      `DevicePolicyManager.setPackagesSuspended()` blocks apps at the system level,
      no heuristics. Careful: your app must be able to deactivate itself afterwards
      (`clearDeviceOwnerApp()`).
- [ ] Or root + LSPosed if the bug has bitten you (direct hook into the Reels screen:
      the most robust thing there is, but it requires root).
- [ ] Distribution: `adb install` or build a signed APK. Google's developer
      verification (in progress 2026-2027) **does not affect** adb installs or
      direct sideloading for now; to share with up to 20 devices there is a free
      "limited distribution account".

---

## Risks and things this plan does not promise

- **OEMs:** some manufacturers kill background services relentlessly; that is why
  Foco is excluded from battery optimization and the limits path does not depend on exact real time.
- **Instagram changes the UI:** hence JSON rules + decision log; expect
  periodic maintenance of Phase 4.
- **Play Store:** accessibility + overlay together is a high-scrutiny area. This
  project is for personal use: it is installed over USB, it is not published.
- **Battery:** the cost is paid by the Phase 3 foreground service; it is measured and tuned there.

## Resources

- Free official course: *Android Basics with Compose* (developer.android.com).
- Key docs: `UsageStatsManager`, `AppOpsManager`, `AccessibilityService`,
  `TYPE_APPLICATION_OVERLAY`, `DevicePolicyManager.setPackagesSuspended`,
  foreground service types (`specialUse`).
- Apps to study their UX: **one sec**, **ScreenZen**, **Scrolless**.
