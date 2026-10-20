# Proposal — One header, and the week stops being a submenu

## Why

The week screen's header is the only one that disagrees with the rest of the app. Settings and an
app's detail share one shape — a `Box` with the back arrow at `Alignment.CenterStart`, a 4 dp inset,
and the title centred (`SettingsScreen.kt:63-89`, `AppDetailScreen.kt:133-166`) — while the week
puts the arrow and the title in a `Row` inside a column that already pads 20 dp
(`WeekScreen.kt:117-140`). The arrow lands 24 dp from the edge, the title is left-aligned and its
style is `headlineMedium` instead of `titleLarge`. Nothing explains the difference except that the
week was written later.

The same review settled what the week and a dated day *are*: not submenus. The home is where a day
is read, and these two are the same reading looked at through another window. So the shared header
is the home's — title plus its actions — and the two of them lose the back control.

## What Changes

- One composable, `ui/common/ScreenHeader.kt`, carries the header: a title, an optional subtitle, an
  optional leading back control and optional trailing actions.
- Settings and an app's detail render through it with the back control (same 4 dp inset, same centred
  title) — the geometry stops being duplicated in two files.
- The home renders through it without the back control, and keeps **both** trailing controls on every
  position it shows: the calendar control (jump to the week) and the settings gear. The week and a
  dated day render through it without the back control too, with the same two actions.
- The week's 20 dp column padding stops applying to the header: it moves to the content, as Settings
  already does.

## Expected outcome

Every screen's header is the same component with the same inset, the same title style and the same
row height, so the week no longer looks like a screen that was bolted on. We will know it was worth
it when a new screen is added by calling one composable instead of copying a `Row`, and when the
week's arrow no longer sits twice as far from the edge as the one in Settings.

## Alternatives considered

| Option | Why yes | Why not / status |
|---|---|---|
| **Extract one header composable with an optional leading control (chosen)** | Kills the divergence at the source, keeps the submenu screens intact, one place to change the geometry | Touches four screens and their tests |
| Fix only the week's `Row` (copy Settings' numbers) | One file, no new component | Leaves three copies of the same geometry; the next screen copies one of them again — **rejected by the human** ("extraélo") |
| Give the week the home's header and leave Settings untouched | Smallest change for the peer screens | Keeps two almost identical headers and the 20 dp/4 dp mismatch in the submenus |

## Non-goals

- No navigation change: the week is still opened from the calendar control and closed with the back
  action (the day ring is `day-ring-navigation`, which depends on this change).
- No new visual language: the same `IconButton`, icon and content description the app already uses.
- No change to what any screen shows.

## Impact

- Removes a visible control from the week and from a dated day: the way back is the system back
  action. The living requirement "A day bar opens that day's view" keeps holding (the back action
  still returns to the dashboard).
- `ui/week/WeekScreen.kt` and `ui/dashboard/DashboardScreen.kt` are also targets of
  `day-ring-navigation`, which declares a dep on this change: sequenced, not in conflict.
- No protected zone (`openspec/config.yaml` → `protected: []`).
