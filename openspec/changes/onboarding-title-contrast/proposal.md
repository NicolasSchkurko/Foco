# Proposal — onboarding-title-contrast

## Why

On a dark appearance, the onboarding screen's title ("Foco") and explanation are painted with the default black content colour while the activity window is painted with the theme's near-black `surface` (`MainActivity.kt:65-70`, `MainActivity.kt:107-110`). `OnboardingScreen` is the only screen without a `Scaffold`/`Surface` shell (`OnboardingScreen.kt:30-48`), so it never receives a themed `LocalContentColor`; the text is black on black and unreadable.

## What

Host the onboarding content in the same themed `Scaffold` shell used by the other screens, so the title and explanation inherit the theme's content colour and remain legible in both light and dark appearances. Copy, permission flow, navigation and storage stay unchanged.

## Expected outcome

Opening Foco without usage access on a dark appearance shows the onboarding title and explanation with readable contrast; the light appearance keeps the same readable contrast. No new colours, dependencies or data.

## Alternatives considered

- **Set only the title's `color`:** fixes the reported word but leaves the explanation, which shares the same default colour, unreadable. Rejected.
- **Change the window background to a light colour:** would undo the themed-window decision (`DECISIONS.md:272-277`) and regress the dark appearance. Rejected.
- **Add a screen-specific background:** duplicates the `Scaffold`/`MaterialTheme` shell already used by the rest of the app (`SettingsScreen.kt:63`, `DashboardScreen.kt:152`). Rejected.
