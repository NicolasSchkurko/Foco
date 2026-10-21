# Spec Delta

## ADDED Requirements

### Requirement: The block is re-validated right before it shows
A block request SHALL be re-validated against the tracked foreground after the check reads its stored data and usage, immediately before the overlay is shown: if the foreground changed while those reads ran, the request SHALL be dropped instead of showing an overlay over another app.
[@test] `app/src/test/java/com/foco/app/domain/ForegroundAppStateTest.kt`

#### Scenario: The foreground changes during the reads
- **WHEN** the entry gate passed and another app comes to the front while the check reads the limit, window and usage
- **THEN** no overlay is shown, the request is dropped, and the app now in front is untouched

#### Scenario: Foco comes to the front during the check
- **WHEN** Foco itself takes over while the check reads its data
- **THEN** the request is dropped and Foco stays open, without a block starting for the previous app
