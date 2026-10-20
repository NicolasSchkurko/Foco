# Tasks — header-alignment

## 1. The component

- [x] 1.1 `ui/common/ScreenHeader.kt`: `ScreenHeader(title, subtitle = null, onBack = null, actions = {})`
      — a `Box(fillMaxWidth)` with the back control at `CenterStart` (4 dp inset) when `onBack` is not
      null, the text block centred, and the actions at `CenterEnd`; no horizontal padding of its own.
- [x] 1.2 The title style is the one Settings and the detail already use (`titleLarge`); the subtitle
      keeps `bodyMedium` on `onSurfaceVariant`.

## 2. The four screens

- [x] 2.1 Settings and the app detail render through `ScreenHeader` with `onBack`, keeping their
      current content and their current geometry (no visual change expected).
- [x] 2.2 The home renders through it without `onBack`; the calendar control (jump to the week) and
      the settings gear become its trailing actions, both present on every position it shows, and a
      dated home loses the arrow it shows today.
- [x] 2.3 The week renders through it without `onBack`; its 20 dp horizontal padding moves from the
      `Column` to the content below the header, as Settings already does.

## 3. Tests

- [x] 3.1 `WeekScreenTest`: the week shows no back control; the header title is present.
- [x] 3.2 `SettingsScreenTest`: the back control is present and returns to the dashboard.
- [x] 3.3 `AppDetailNavigationTest`: the detail's back control still returns to the dashboard (the
      regression: the shared header must not break it).

## 4. Verification

- [x] 4.1 `gate-check --run-tests` green (JVM + instrumented).
- [x] 4.2 `openspec validate --all --strict` clean.
- [ ] 4.3 Screens checked by eye on the emulator: home, week, dated day, settings and detail side by
      side, same inset and same title size.
