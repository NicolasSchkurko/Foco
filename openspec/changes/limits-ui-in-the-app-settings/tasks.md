# Tasks — limits-ui-in-the-app-settings

- [x] 1. Extract the quick adjust and the time-range picker into `ui/common/LimitsQuickAdjust.kt`
      (public, reusable), with the alert toggle next to it as `LimitAlertsToggle`.
- [x] 2. `AppAppSettingsScreen` uses the new editor; `AppLimitEditor` and its test are deleted.
- [x] 3. The global `SettingsScreen` loses its limits item and the now-unused imports.
- [x] 4. The app detail's limits preview is tappable (opens that app's settings) and carries a
      `testTag` so the test does not depend on the language; the header icon is the limits icon.
- [x] 5. The dashboard loses the alert icon that was added for a card it does not have.
- [x] 6. The swipe-to-back gesture is rebuilt on `awaitPointerEventScope` + `PointerEventPass.Initial`.
- [x] 7. Regression: `LimitsQuickAdjustTest` (new), `AppDetailLimitsTest` (rewritten),
      `AppAppSettingsTest` (updated), `AppLimitBlockTest` (the overlay owner moves on the main
      thread, as production does).
- [x] 8. `gradlew.bat test` green; `gate-check --run-tests` records evidence.

- [x] 8. The detail's horizontal swipe walks the days (swipe left to the next, older day; swipe
      right to the previous one), like the home's ring, instead of leaving the screen; the back
      control still leaves the detail.
- [x] 9. The daily limit is a switch plus its size: off means no limit, on means 15 minutes by
      default; zero is shown as "block now" and never as the absence of a limit.
- [x] 10. The alerts toggle follows the limit (available with any limit, including "block now")
      and the limits page gained the descriptions the M3 settings pattern asks for.
- [x] 11. Regression: `AppDetailLimitsTest` (the swipe walks the days, the limits block and the
      limits icon open the app's settings), `LimitsQuickAdjustTest` (the switch, the disabled
      stepper, the floor), `AppAppSettingsTest` (the switch persists).
