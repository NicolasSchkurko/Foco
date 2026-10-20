# Tasks — onboarding-title-contrast

- [x] 1. Add `OnboardingContrastTest` under `app/src/androidTest/java/com/foco/app/ui/`: render onboarding with `FocoTheme(darkTheme = true)` and `false` over a `surface` backdrop, capture the title and explanation, and assert 3:1 for the display title and 4.5:1 for the body text. Confirm the dark case fails against the current code.
- [x] 2. Wrap the existing `OnboardingScreen` content in the same themed `Scaffold` shell used by `SettingsScreen.kt:63` and `DashboardScreen.kt:152`; keep the `Column`, copy, button and settings intent unchanged.
- [x] 3. Let the title and explanation inherit `LocalContentColor`; do not add hardcoded colours. Keep the hint's `onSurfaceVariant` only if it still meets its contrast threshold.
- [x] 4. Run `gradlew.bat test` and the instrumentation test on the connected device/emulator; record the result.
- [x] 5. Run `openspec validate onboarding-title-contrast --strict` and `node scripts/gates/build-board.mjs`.
