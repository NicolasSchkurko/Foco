# notification-permission-toggle — tech brief

## State and persistence mechanism

The notification permission is system-managed Android state (`POST_NOTIFICATIONS`). It is not stored in app-private storage (no Room, DataStore, or file). The app reads it only through `ContextCompat.checkSelfPermission()` and updates its `notificationsEnabled` mutable state via the activity result launcher (`rememberLauncherForActivityResult` for `RequestPermission()`). Changing the permission outside the app updates the state on the next `Lifecycle.Event.ON_RESUME` check.

## Concurrency

The launcher callback updates a single `remember` mutable state; the `onLimitChange` and `onAlertChange` actions run inside `rememberCoroutineScope()` and launch `AppLimitsStore` writes on `Dispatchers.IO`. The request is synchronous with the user action (tap triggers `launch`), and the callback completes before the next tap can occur (Compose single-threaded dispatch). No concurrent writes to `limits.db` occur because `AppLimitsStore` uses Room with internal locking.

## Networking / persistence / new dependency

No network, no new SDK, no new dependency added. Persistence is only the existing Room `limits.db` (written through `AppLimitsStore.setLimit`, `setWindow`, `setAlertEnabled`). The manifest does not add `POST_NOTIFICATIONS` (it was already present); no `android.permission.INTERNET` is added.

## Security / platform SDK

Uses the standard Android permission launcher (`ActivityResultContracts.RequestPermission`) and `ContextCompat.checkSelfPermission()`. No opaque implementation: the permission request is visible to the user and the system handles the dialog. No shortcut that skips user consent.

## Mechanism

The change removes `requestNotificationPermission()` from `onLimitChange` and keeps it in `onAlertChange`. The launcher (`rememberLauncherForActivityResult`) is declared once inside `SettingsScreen`; the callback updates `notificationsEnabled`. No other mechanism is altered.

## Physical traceability

- `SettingsScreen.kt`: line 196 (before edit) removed; line 219 (`onAlertChange`) preserved.
- `walkthrough.md`: this file (created at apply).
- `tests`: `AppLimitsSettingsTest` covers toggle/permission behavior; `AppDetailLimitsTest` covers preview.

## Limits and failure modes

- The permission request is system-managed; if the user denies it, `notificationsEnabled` stays false and the toggle becomes disabled (existing `AppLimitRow` rule). The block/monitor still works because it does not require notifications.
- Failure mode: launcher not registered (missing `rememberLauncherForActivityResult`) — prevented by existing code; change only removes a call, doesn't add new launcher usage.

## Touched APIs

- `Android.Manifest.permission.POST_NOTIFICATIONS` (system permission, no SDK change).
- `androidx.activity.result.contract.ActivityResultContracts.RequestPermission()` (existing launcher).
- `ContextCompat.checkSelfPermission()` (existing check).
- `AppLimitsStore.setLimit()` / `clearLimit()` (unchanged calls).

## Implicit decisions

- The fix scopes exclusively to the toggle/permission coupling; no broader permission-model redesign (e.g., no `NotificationChannel` changes).
- No new database column or service change required.

## Cognitive sign-off

Understood: the permission should only be requested when the alert feature is opted in, not when configuring the daily limit.

## Comprehension sign-off

Understood: the permission request moves from the limit-change path to the alert-toggle activation only. The change is a single removal (`requestNotificationPermission()` from `onLimitChange`) with regression covered by `AppLimitsSettingsTest`.
