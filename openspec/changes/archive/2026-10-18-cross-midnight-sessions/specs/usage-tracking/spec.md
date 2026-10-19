# Spec Delta

## ADDED Requirements

### Requirement: A session already open at the day's start counts from the day's start
Usage sessions SHALL include an app that was already in the foreground when the day began: the day's query SHALL look back far enough to find the event that opened the session, and the resulting session SHALL be clamped to the day's start so the previous day keeps its own slice.
[@test] `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`

#### Scenario: An app open across midnight
- **WHEN** an app resumes at 23:50 and is still in the foreground when the next day is queried at 00:06
- **THEN** the new day yields a session for that app starting at 00:00 and ending at 00:06

#### Scenario: The previous day keeps its own slice
- **WHEN** the same app is queried for the previous day
- **THEN** its session there ends at that day's end, and the minutes are counted once and only once
