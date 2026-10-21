# Spec Delta

## ADDED Requirements

### Requirement: A restored position renders its content without a placeholder
The home SHALL keep the content of a position it has already shown across recreation and navigation: returning from Settings or any recreation that restores the position SHALL render that content directly, with no loading placeholder. The placeholder stays reserved for the first load — a position whose content was never shown, as on a cold start.
[@test] `app/src/androidTest/java/com/foco/app/ui/DashboardRingTest.kt`
[@test] `app/src/test/java/com/foco/app/ui/dashboard/RingMotionTest.kt`

#### Scenario: Return from settings
- **WHEN** the home comes back from Settings with its previous position restored and that position's content was already shown
- **THEN** the content renders immediately, with no spinner, no loading text and no reload flash

#### Scenario: Recreation keeps the content
- **WHEN** the activity is recreated while a day or week position is on screen
- **THEN** the same position comes back with its content already composed, without the placeholder

#### Scenario: The first load still loads
- **WHEN** the home has never shown any position (cold start)
- **THEN** the loading placeholder appears until that position's content is ready, exactly as today
