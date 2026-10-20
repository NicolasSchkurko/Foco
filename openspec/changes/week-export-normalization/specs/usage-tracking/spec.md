# Spec Delta

## ADDED Requirements

### Requirement: Stored days are normalized in the export
The week export SHALL apply the same contiguous same-app `HH:mm` normalization to stored days as the app views. A stored pair that meets at the same `HH:mm` SHALL appear as one session with the earliest start and latest end; different apps or days SHALL NOT be merged.
[@test] `app/src/test/java/com/foco/app/domain/WeekExportTest.kt`

#### Scenario: A stored same-minute pair
- **WHEN** a stored day has app A from 03:44:10 to 03:44:50 and again from 03:44:55 to 03:46:00
- **THEN** the exported day has one A session from 03:44:10 to 03:46:00

#### Scenario: Today is unchanged
- **WHEN** today's live sessions are already normalized
- **THEN** the export applies the same rule idempotently and still shows one session

#### Scenario: Different apps or days
- **WHEN** A ends at 03:44 and B starts at 03:44, or A ends on one day and starts at 03:44 the next
- **THEN** the export does not merge them
