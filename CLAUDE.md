# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app that captures and displays key events from all input sources except touch. Also listens to the `com.thorkracing.wireddevices.keypress` broadcast intent. Displays per event: key code, key symbol, event type (down/up), device name, class, and source.

**Status:** Template placeholders (`keytester`) have not been replaced yet. Before any feature work, rename the package, app label, and file paths to the real app name.

## Build & CI

All builds run in CI/CD — do not attempt local builds (AGP is inaccessible due to firewall).

| CI task | Trigger |
|---|---|
| Lint + debug APK | Push to `main`, or PR labeled `run-build` |
| Signed release APK + GitHub draft release | Manual `workflow_dispatch` |

Signing secrets required: `SIGNING_KEYSTORE_BASE64`, `SIGNING_KEYSTORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.

## Architecture

- **Single Activity** (`MainActivity`) — no fragments, no navigation component.
- **Jetpack Compose** UI only — no XML layouts.
- **minSdk 26** (Android 8.0) — no need for pre-Oreo compatibility paths.
- Key input capture: override `dispatchKeyEvent()` in `MainActivity`; collect events into Compose `State`.
- Broadcast receiver for `com.thorkracing.wireddevices.keypress` must be registered dynamically (not in the manifest) and unregistered in `onDestroy`.

## Rules

- Always commit every logical step. Do not batch unrelated changes into one commit.
- Always rebase the working branch onto `main` at the end of a task.
- Maintain `CHANGELOG.md` (keep-a-changelog format) after each task.
- Maintain `.github/development-journal.md` with stack info, key decisions, and core features.

## Git

Configure before any git operation:
- `user.name = c0dev0id`
- `user.email = sh+git@codevoid.de`

No `Co-Authored-By` or other attribution lines in commits or PRs. Remove any lines containing "claude" from commit/PR messages. If `.gh_token` is present, use it for GitHub API access.
