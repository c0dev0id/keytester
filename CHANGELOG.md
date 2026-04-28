# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Key event logging from hardware input devices via `dispatchKeyEvent`
- Broadcast receiver for `com.thorkracing.wireddevices.keypress` intent (key_press / key_release extras)
- Press duration display in milliseconds (hardware: from `KeyEvent.downTime`; broadcast: tracked per device+keycode)
- Unrecognised broadcast payloads logged with raw extras for fault diagnosis
- Clear button to reset the log
- Share button to export the log as plain text via system share sheet
- Newest events shown at top; log capped at 500 entries
