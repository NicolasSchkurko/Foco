# Tasks — home-ring-polish

- [x] 1. Add a `slideDirection` state to `DashboardScreen`; set it from `moveStep`, `goBack`, and the
      ring index when a bar or the calendar moves the position.
- [x] 2. Wrap the position content in `AnimatedContent(targetState = stopKey)` with a short
      `slideInHorizontally`/`slideOutHorizontally` plus fade in the `slideDirection` direction.
- [x] 3. Remove the subtitle from `ScreenHeader`; render the `dashboard_usage_line` between the `‹`
      and `›` controls, centered, with the title vertically centered against the actions.
- [x] 4. Extend `DashboardRingTest` to measure the legend between the arrows and the title's vertical
      center against the settings action.
- [x] 5. Run `gradlew.bat test`, `DashboardRingTest`, `WeekScreenTest` and `gate-check --run-tests`.
- [ ] 6. Install the debug APK on the Pixel and walk forward/back by arrow and swipe.
