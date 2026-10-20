# Spec Delta

## ADDED Requirements

### Requirement: Onboarding text stays legible over the themed window
The onboarding screen SHALL render its title and explanation with the theme's content colour so the display title reaches at least 3:1 contrast and the body text at least 4.5:1 against the themed background, in both light and dark appearances.
[@test] `app/src/androidTest/java/com/foco/app/ui/OnboardingContrastTest.kt`

#### Scenario: Dark appearance
- **WHEN** the onboarding screen is shown with the dark appearance active
- **THEN** the title and explanation meet their contrast thresholds against the near-black surface, never black on black

#### Scenario: Light appearance
- **WHEN** the onboarding screen is shown with the light appearance active
- **THEN** the title and explanation meet their contrast thresholds against the light surface
