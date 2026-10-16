# Proposal — Long-press to exclude an app from the ranking

## Why

The ranking can fill with apps the user never wants to look at. The user wants to long-press an app in the ranking and send it to the excluded packages list — a personal, persisted list that declutters the dashboard without lying about the numbers.

## What Changes

- **Long-press an app in the ranking** → it is excluded: it disappears from the ranking, while its time **keeps counting** in the day total and the hourly chart (human decision: ranking-only exclusion; the deliberate side effect is that rows no longer sum to the total).
- **Immediate recovery** (human decision): an **Undo** snackbar. Managing old exclusions lives in the upcoming Settings screen (`app-settings` change — single management home); this change keeps only the gesture and its undo.
- The list **persists across restarts** via **DataStore Preferences** (human decision, chosen over SharedPreferences and Room).
- The pure ranking derivation gains an excluded-packages input; day total and hourly distribution stay untouched (unit-tested).
- Non-goals: changing what the repository collects or the session filter (`self`/home stay built-in there); per-app notifications.

## Expected Outcome

"Dashboard sin ruido y la lista persistente entre reinicios": one gesture removes an app from the ranking, one tap undoes it, and the list survives restarts (full management lands in the Settings screen).

## Alternatives Considered

| Meaning of "exclude" | Summary | Why not chosen |
|---|---|---|
| **Ranking-only — chosen** (human decision) | App hidden from the ranking; totals/hours keep counting it | Rows no longer sum to the total (deliberate) |
| Exclude from everything (like `self`/home in the session filter) | Consistent with the existing filter semantics | The user wants the number honest: the time happened |

| Recovery UX | Summary | Why not chosen |
|---|---|---|
| **Undo here; management in the Settings screen — chosen** (human decision) | Immediate undo now; the excluded manager lives in the `app-settings` change (single home) | — |
| "Excluded" section on the dashboard | Quick access | Two places managing the same list; dashboard clutter |
| Toggle in the app-detail screen | Elegant reuse | Couples this change to the `app-usage-detail` draft |

| Persistence | Summary | Why not chosen |
|---|---|---|
| **DataStore Preferences — chosen** (human decision) | Modern Google idiom; coroutines/Flow native; good learning | New dependency + Flow plumbing for a small set |
| SharedPreferences | Zero dependencies; enough for a string set | Legacy API; no Flow |
| Room | The Phase 2 persistence system | Overkill for a package set; drags Phase 2 early |

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `usage-tracking`: adds ranking exclusions (gesture, ranking-only semantics, persistence, management).

## Impact

- `app/src/main/java/com/foco/app/ui/**` — long-press on `AppUsageRow`, undo snackbar, dashboard wiring of the excluded set
- `app/src/main/java/com/foco/app/data/**` — a thin DataStore-backed store for the excluded packages
- `app/build.gradle.kts`, `gradle/libs.versions.toml` — `androidx.datastore:datastore-preferences`
- `app/src/test/**`, `app/src/androidTest/**` — ranking-derivation tests + excluded-apps flow tests
- Ordering: **depends on `app-usage-detail`** (human decision) — this lands after it; overlaps with `app-entry-gate` (`androidTest/**`) stay informational
