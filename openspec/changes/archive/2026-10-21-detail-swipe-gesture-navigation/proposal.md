# Proposal — detail-swipe-gesture-navigation

## Why

User feature request (not from anchored audit): add touch navigation inside the app detail: a horizontal edge swipe / fling for closing or moving through elements.

## What changes

- `AppDetailScreen.kt`: a gesture detector (`PanGesture` or native fling) on the content container with a 30% width / 500 px/s threshold; a rightward edge swipe triggers the back/close action; a horizontal fling navigates between detail elements when applicable.
- `AppDetailNavigationTest.kt`: verifies the swipe triggers the expected navigation; the gesture ignores vertical scrolls (dead zone).
- No contract change to existing specs; `skip_specs: true` (pure UI interaction, no contract delta required).

## Alternatives

| Option | Verdict |
|---|---|
| Native `PanGestureHandler` with configurable threshold | **Chosen** |
| External library for swipe | Not needed; native is sufficient |

## Origin

Filed 2026-10-21 by user request (feature, no verified finding); draft — awaiting review.
