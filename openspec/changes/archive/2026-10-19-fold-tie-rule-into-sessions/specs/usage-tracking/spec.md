# Spec Delta

## MODIFIED Requirements

### Requirement: Usage sessions are built from system events
The system SHALL build per-app usage sessions (package, start, end) for the day from resume/pause/stop events, over plain event data: the direct switch closes the previous session at the new start and a pause or stop closes the open session of its own package (both ties-inclusive); a session open at query time closes at "now"; same-app transitions do not split it; a zero-length session is not emitted.
[@test] `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`

#### Scenario: Direct switch between apps
- **WHEN** app A resumes at 10:00 and app B resumes at 10:05
- **THEN** A yields the session 10:00–10:05 and B's session starts at 10:05

#### Scenario: A direct switch inside the same millisecond
- **WHEN** app A resumes and app B resumes at the same millisecond
- **THEN** A's session closes at that instant and B's session opens there, with A contributing no time

#### Scenario: Session open at query time
- **WHEN** an app is in the foreground when the day is queried
- **THEN** its session ends at the query time

#### Scenario: A stop in the same millisecond as the resume
- **WHEN** an app resumes and stops at the same millisecond
- **THEN** no session is emitted for it, and no package is left open afterwards

#### Scenario: No activity
- **WHEN** there are no relevant events for the day
- **THEN** the session list is empty

## REMOVED Requirements

### Requirement: A transition settles by event order, not by strict inequality
**Reason**: Its clauses are the same fact the requirement *Usage sessions are built from system events* states — the direct switch — plus the stop and zero-length halves, which now live there too; one fact, one owner.
**Migration**: Same `[@test]` (`app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`) and the same two scenarios, folded into the requirement above. Closes the medium finding of the archived `2026-10-18-same-millisecond-transitions` (`findings.md`).
