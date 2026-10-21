# Proposal — limits-ui-in-the-app-settings

## Why

The limits editor lives in the wrong place. After `settings-limits-quick-adjust` the quick-adjust
buttons and the time-range picker were added to the **global** Settings screen, where they edit
Foco's own package (`context.applicationContext.packageName`) — a row nobody asked for. The screen
that actually configures an app's limit and window is the **app's** settings tab
(`AppAppSettingsScreen`), and it still had the old dropdown editor (`AppLimitEditor`).

Three symptoms followed from that split:

- The app's settings tab showed the old UI while the global tab showed the new one.
- The dashboard grew an alert icon for a card that does not exist there.
- The app detail's limits preview was a dead end: it showed the values but tapping them did nothing.

## What changes

- The quick adjust (`-15 min` / `+15 min` with long-press acceleration) and the time-range picker
  move to the **app's** settings tab, as a shared component (`ui/common/LimitsQuickAdjust.kt`); the
  old dropdown editor (`AppLimitEditor`) is deleted.
- The global Settings screen loses its limits item: the quick adjust is now where the limits are.
- The app detail's limits preview block is fully tappable and opens that app's limit settings; the
  header action becomes the limits icon instead of a gear.
- The dashboard loses the alert icon that was added for a card it does not have.
- The detail's swipe-to-back gesture is rebuilt on `awaitPointerEventScope` with
  `PointerEventPass.Initial`, so a horizontal swipe is seen even when it starts on the scrollable
  list, while a vertical drag still scrolls.

## Alternatives

| Option | Effect | Verdict |
|---|---|---|
| Move the editor to the app's tab, delete the old one | one editor, in the place the limits are | **Chosen** |
| Keep both | two editors for the same data, one of them editing Foco itself | not proposed |
| Keep the old editor in the app's tab and drop the quick adjust | the old UI stays | not proposed |

## Risks

- The global Settings screen loses a visible section (it configured Foco's own limit, which has no
  user-facing meaning).
- The gesture now runs on the Initial pass: it must not steal the list's vertical scroll. The
  implementation only consumes once the movement is horizontal, and the vertical case hands the
  gesture back to the list.

## Origin

Filed 2026-10-21 by user instruction (the quick adjust and the time range belong in the app's
settings tab, not in the global one); `kind: feature`; no contract change to the limits
themselves.
