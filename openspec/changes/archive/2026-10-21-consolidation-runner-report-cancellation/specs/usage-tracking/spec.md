# Spec Delta

## MODIFIED Requirements

### Requirement: Cancelling one trigger does not cancel the shared run
A cancellation of one caller SHALL stop only that caller's wait; it SHALL NOT cancel an in-flight consolidation started for another caller. A waiting nightly worker SHALL still receive `Done` or `Failed` and SHALL retry on failure. If the shared run itself ends in cancellation, the runner SHALL report it as `Failed` so the worker retries instead of propagating the cancellation.
[@test] `app/src/test/java/com/foco/app/data/ConsolidationConcurrencyTest.kt`

#### Scenario: The startup wait is cancelled
- **WHEN** the app startup effect is cancelled while the nightly worker waits for the same run
- **THEN** the run continues and the worker receives its outcome

#### Scenario: Two overlapping triggers
- **WHEN** two trigger callers overlap
- **THEN** the store is still executed once and both receive the same outcome

#### Scenario: The worker retries a failure
- **WHEN** the shared run ends in failure
- **THEN** the worker returns a retry result, not a cancellation