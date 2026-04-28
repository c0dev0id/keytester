# Development Journal

## Software Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material3), Compose BOM 2026.02.01
- **Min SDK:** 26 (Android 8.0)
- **Target/Compile SDK:** 36
- **Build:** CI-only via GitHub Actions (AGP 9.1.0, Kotlin Compose plugin 2.3.10)

## Key Decisions

**Broadcast receiver registered dynamically (onResume/onPause), not in the manifest.**
Android 8.0+ restricts implicit broadcast receivers declared in the manifest for custom actions. Dynamic registration is required for `com.thorkracing.wireddevices.keypress`.

**`dispatchKeyEvent` used for hardware key capture, not `onKeyDown`/`onKeyUp`.**
`dispatchKeyEvent` fires for every action including ACTION_MULTIPLE and unknown action codes, which is necessary for a diagnostic tool that must log faulty/incomplete signals. `onKeyDown`/`onKeyUp` only cover the common cases.

**Hardware press duration from `KeyEvent.downTime` / `eventTime`, not a tracking map.**
`KeyEvent` carries both timestamps natively. A tracking map is only needed for broadcast events where we receive separate press/release intents.

**Unrecognised broadcast payloads logged with raw extras.**
The receiver logs any broadcast with the keypress action even if it lacks the expected `key_press`/`key_release` keys, displaying all extras as-is. This ensures faulty or extended payloads are never silently dropped.

**`LazyColumn(reverseLayout = true)` with append-only list.**
Prepending to a `SnapshotStateList` is O(n). Appending is O(1). Reversing the layout gives newest-first display without any list mutation cost.

**Log cap at 500 entries.**
Prevents unbounded memory growth during long test sessions. Oldest entries are dropped first.

## Core Features

- Log all key events from hardware input devices (keycode, symbol, action, device name, source class, input source, press duration)
- Log `com.thorkracing.wireddevices.keypress` broadcast events from andRemote2 (key_press / key_release / deviceName extras)
- Log unrecognised broadcast payloads with raw extras for fault diagnosis
- Clear log
- Share/export log as plain text
