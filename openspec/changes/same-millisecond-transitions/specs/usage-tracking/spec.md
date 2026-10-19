# Spec Delta

## ADDED Requirements

### Requirement: A transition settles by event order, not by strict inequality
A resume for a different package SHALL close the open session at that resume's timestamp, and a pause or stop SHALL close the open session of its own package, in both cases even when the event shares the session's start millisecond. A session whose start equals its end SHALL NOT be emitted.
[@test] `app/src/test/java/com/foco/app/domain/SessionBuilderTest.kt`

#### Scenario: A direct switch inside the same millisecond
- **WHEN** app A resumes and app B resumes at the same millisecond
- **THEN** A's session closes at that instant and B's session opens there, with A contributing no time

#### Scenario: A stop in the same millisecond as the resume
- **WHEN** an app resumes and stops at the same millisecond
- **THEN** no session is emitted for it, and no package is left open afterwards
