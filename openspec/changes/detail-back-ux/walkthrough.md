# Walkthrough — detail-back-ux (tech brief)

> For the human: this brief explains the mechanism of the change. Answer the questions in
> `## Cognitive sign-off` yourself; the agent never fills them.

## Mechanism

Android routes "back" differently depending on the platform level. Up to Android 15, a back
key/gesture arrived at the activity as a key event, which androidx forwards to the
`OnBackPressedDispatcher` — the dispatcher our navigation listens to (`NavHost` pops the stack).
Since **Android 16 (API 36), predictive back is enabled by default for apps targeting 36**: the
system back *gesture* is delivered through a different door — an `OnBackInvokedCallback` that an
app must have registered. Our app never declared `android:enableOnBackInvokedCallback`, so the
activity did not register that callback: the **gesture** had no listener (while the **key** and
the **visible control** kept working — both were verified empirically). The fix declares the flag
in the manifest; the activity then registers a callback that forwards onto the same
`OnBackPressedDispatcher` — one consistent path for gesture, key and control. The second part of
the change swaps the text glyph (`←`) for the Material icon `Icons.AutoMirrored.Filled.ArrowBack`
with a `contentDescription`, so the control is standard and accessible.

## Physical traceability

- `app/src/main/AndroidManifest.xml` — declarative platform contract read by the OS at install
  time; the flag lives on `<application>`.
- `app/src/main/java/com/foco/app/ui/dashboard/AppDetailScreen.kt` — Compose UI; the control
  calls the current `OnBackPressedDispatcher` (`LocalOnBackPressedDispatcherOwner`).
- `app/src/test/java/com/foco/app/BackInvokedCallbackManifestTest.kt` — JVM test; reads
  `src/main/AndroidManifest.xml` relative to the module dir (Gradle unit-test working dir).
- `app/src/androidTest/java/com/foco/app/ui/AppDetailNavigationTest.kt` — instrumented E2E on the
  API 36 emulator; finds the control by its content description.

## Limits and failure modes

- The manifest test asserts the **flag is present**; it cannot exercise the gesture itself —
  synthetic edge-swipes via adb are not reliable (proven while measuring this bug). The gesture
  behavior is verified manually on device and listed in the tasks.
- If the flag is removed (or a tool regenerates the manifest), the gesture breaks silently again
  — the JVM test is the tripwire.
- Predictive-back **animations** are not implemented; the gesture completes without a peek
  animation. Acceptable for this app; a full migration would be its own change.
- Below API 33 the flag is ignored: behavior is unchanged on the supported floor (minSdk 29).
- The icon swap has no state; if `material-icons-core` is already on the classpath the
  dependency entry is skipped.

## Touched APIs

- `android:enableOnBackInvokedCallback` (manifest attribute; platform back APIs, Android 13+).
- `OnBackPressedDispatcher` / `LocalOnBackPressedDispatcherOwner` (androidx activity-compose).
- `Icons.AutoMirrored.Filled.ArrowBack` + `Icon` (Compose Material; likely needs
  `androidx.compose.material:material-icons-core`, BOM-managed).
- `contentDescription` semantics for the control.

## Implicit decisions

- **One change, not two**: the fix and the icon touch the same screen and landed together at the
  user's request; the kind is `fix` because the gesture bug drives it (regression test
  mandatory).
- **Manifest contract test as the regression**: the gesture itself cannot be automated reliably;
  the test pins the cause (the flag), and the dispatcher path is already covered by the
  navigation tests.
- **Pre-declared ordering**: `app-settings` waits for this change (deps); `app-entry-gate`
  follows transitively. Flagged to the human, reversible with a new decision entry.

## Cognitive sign-off

Answer in your own words (the agent must not fill these):

1. Why did the back gesture stop returning from the detail on Android 16 even though the app
   code had not changed?
   > Basicamente android cambio el gesto de mirar atras a un callback nuevo que hay que declarar
   > en el manifiesto como enable true, caso contrario el ir para atras no funciona.
   >
   > — the human, 2026-10-16
2. In one sentence: what does declaring `android:enableOnBackInvokedCallback="true"` change?
   > ok al declarar la bandera ahora el gesto entrega la accion al callback y el callback a la
   > funcion de on back pressed dispatcher
   >
   > — the human, 2026-10-16
3. If the gesture breaks again after a future update, what is the first thing you would check?
   > Tanto el manifiesto como el como se handlea el callback en nuestro proyecto
   >
   > — the human, 2026-10-16

When satisfied, the sign-off is recorded in `/spec-verify` (`understood_by` + `understood_at` in
`.openspec.yaml`).
