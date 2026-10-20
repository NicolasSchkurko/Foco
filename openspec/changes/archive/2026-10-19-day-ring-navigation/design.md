# Design — day-ring-navigation

## The ring, exactly

Positions, in the order the **forward** step walks them:

```
today → week → oldest → the day after it → … → yesterday → today
```

- Forward (`›`, or a swipe from right to left) moves one position to the right.
- Back (`‹`, or a swipe from left to right) moves one position to the left.
- The ring has no ends: from yesterday, forward is today; from today, back is yesterday; from the
  oldest day, back is the week; from the week, forward is the oldest day.

Why the week sits between today and the oldest day: that is the human's model. Forward from today is
the summary of the week just lived; forward again enters the stored days at their oldest end. Back
from the oldest day leaves the stored days through the week as well.

## Where the state lives

`domain/DayRing.kt` holds the pure part:

```kotlin
sealed interface DayStop { data class Day(val date: LocalDate) : DayStop; data object Week : DayStop }

fun dayRing(today: LocalDate, storedDays: Set<LocalDate>): List<DayStop>   // forward order
fun DayStop.step(ring: List<DayStop>, forward: Boolean): DayStop          // wraps
```

The home keeps `var stop by remember { mutableStateOf<DayStop>(DayStop.Day(today)) }` plus the
history it came through, and the ring is `remember(today, storedDays)`. The stored days come from the
store stream the detail already uses (`storedDaysFlow`), so a day stored while the home is open joins
the ring without leaving it.

## In-place rendering

The home renders by position:

- a `Day` position renders the day composition the dated home renders today (`DashboardScreen.kt:103`)
  — live for today, one stored read otherwise;
- the `Week` position renders the week's content (bars + ranking).

No navigation happens when the position changes, so nothing is pushed onto the navigation stack. What
moves is one state field plus its **history**: a short list of the positions visited that the back
action pops, one per press, and that empties at today (where back leaves the app). A bar tapped in the
week pushes a position like any other step — a shortcut, not a parent-child edge.

The position must survive a trip to Settings, which is a route: either the state is `rememberSaveable`
in the home's back-stack entry, or it is hoisted above the `NavHost`. The first is enough because the
history is a list of small, saveable values.

The `day/{date}` and `week` routes are deleted: with the ring they would be a second way to the same
content.

## The gesture

- `Modifier.pointerInput(ring) { detectHorizontalDragGestures(...) }` on the content container, with a
  committed threshold (~64 dp) and the dominant axis deciding between a day step and a vertical
  scroll — the app has no horizontal-scrollable content today (grep: no `horizontalScroll`,
  `HorizontalPager` or `draggable` in `app/src/main/java`), so a horizontal drag is unambiguous.
- No edge-only gesture: on Android 11+ the left and right edges belong to the system back gesture,
  and `systemGestureExclusion` is capped, so the swipe is allowed anywhere on the content.
- Accessibility: the arrows are the non-gesture path and keep content descriptions; the swipe changes
  nothing they do not already reach.

## Risks

- **Vertical scroll stealing the drag**: the ranking list scrolls vertically; the gesture is attached
  to the container, and `detectHorizontalDragGestures` only consumes once the horizontal slop is
  passed.
- **A stale ring**: the ring is rebuilt when the stored days change; if the current position
  disappears (a day pruned), the home falls back to today and the history drops the vanished
  positions.
- **Today changing at midnight**: out of scope here, as in `detail-window-follows-store`; the ring is
  built from the `today` captured when the home enters composition.

## Verification

- `DayRingTest` (JVM): order, both wraps, the empty-store ring, the fallback when a position vanishes.
- `DashboardRingTest` (instrumented): swipe forward/back, arrows, calendar control, back retracing the
  visited positions, and Settings returning to the position it was opened from.
- `WeekScreenTest` updated: the week reached from the home is the week position.
