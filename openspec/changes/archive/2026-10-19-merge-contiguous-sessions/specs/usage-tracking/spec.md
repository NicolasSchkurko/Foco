# Spec Delta

## ADDED Requirements

### Requirement: Contiguous same-app sessions are normalized
After the fold, sessions of the same app that meet at the same local `HH:mm` on the same day SHALL be merged into one from the earliest start to the latest end, transitively; different apps or different local days SHALL NOT be merged. The normalization SHALL apply to the live day build and to the app detail read; stored rows may remain raw.
[@test] `app/src/test/java/com/foco/app/domain/SessionMergeTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/metrics/AppDetailMetricsTest.kt`

#### Scenario: The same displayed minute
- **WHEN** app A ends at 03:44:10 and app A starts again at 03:44:55
- **THEN** the detail shows one A session from 03:44:10, not two rows

#### Scenario: A chain
- **WHEN** A runs 04:00–04:30 and then 04:30–04:45
- **THEN** A has one 04:00–04:45 session and a session count of one

#### Scenario: Different app or day
- **WHEN** A ends at 03:44 and B starts at 03:44, or A ends on one day and starts at 03:44 the next
- **THEN** the sessions are not merged

#### Scenario: Stored rows without a migration
- **WHEN** an already-stored day contains two contiguous A rows
- **THEN** the detail read merges them, while the raw rows and daily totals stay unchanged

#### Scenario: Visibility after the merge
- **WHEN** two contiguous A pieces are 500 ms each
- **THEN** the merged 1 s session is visible under the existing sub-second rule
