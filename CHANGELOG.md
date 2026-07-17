# Changelog

All notable changes to this project are documented here. The project does not currently use version tags, so historical entries are grouped by commit date and reference the relevant commit hashes.

## Unreleased

### Added
- Added centralized alarm-clock scheduling with `TimerScheduler`, replacing duplicated `AlarmManager` logic in the activity and boot receiver.
- Added persisted timer reconciliation with `TimerStateReconciler` so launch, resume, boot, and expired-timer recovery follow the same state rules.
- Added `AlarmPlayer` for looping alarm playback with alarm audio attributes and fallback sounds.
- Added a short wake-lock handoff from timer expiry broadcasts to alarm playback so alarms start reliably while the device is asleep.
- Added local unit/Robolectric coverage for timer reconciliation and alarm scheduling.
- Added adaptive icon monochrome metadata for modern launcher support.

### Changed
- Switched timer expiry scheduling to `AlarmManager.setAlarmClock()` for user-visible, lock/sleep-resilient timer alerts.
- Updated alarm foreground-service declarations to use the media playback type and separated active alarm notifications from quiet running-timer notifications.
- Updated compile and target SDK to 36 and refreshed AndroidX, Material, ConstraintLayout, Gson, and Robolectric dependencies.
- Reworked app startup, resume, and boot handling to use persisted state rather than static service state as the source of truth.
- Replaced broad RecyclerView refreshes with `DiffUtil` for timer list updates.

### Fixed
- Fixed timers not always alerting correctly while the phone is locked or asleep.
- Fixed stale pending alarm broadcasts so deleted timers do not start ringing.
- Fixed dark-mode reset button contrast by using an outlined Material button with explicit secondary tint and icon treatment.
- Fixed toolbar/status-bar overlap on modern Android by applying system-bar insets to the app bar, timer list, and floating action button.
- Fixed lint issues around AppCompat tinting, adaptive icons, obsolete SDK annotations, overdraw, and small layout performance warnings.

## 2026-05-30 - Modernize timer app UI and alarm handling (`910389c`)

### Added
- Added `TimerAlarmService` for foreground alarm playback and lockscreen alarm notification handling.
- Added `TimerStore` for persisted timer state.
- Added `BootReceiver` to restore active timers after device reboot.
- Added dark-mode resource variants.

### Changed
- Modernized the UI with Material 3 styling, card-based timer rows, toolbar, floating action button, and updated color/theme resources.
- Expanded timer lifecycle handling for running, firing, resetting, and notification-backed timer states.
- Updated Gradle and Android configuration for the modernized app structure.

## 2026-02-21 - Silent/DND alarm behavior (`55c3bcc`)

### Changed
- Adjusted alarm behavior so timer expiry can alert through silent and Do Not Disturb modes.
- Updated expiry receiver handling to better support alarm-style interruption behavior.

## 2026-02-21 - Repository cleanup (`0b03b4b`)

### Changed
- Updated `.gitignore`.
- Removed generated build outputs from version control.

## 2026-02-21 - Startup and expiry receiver fixes (`6f38a9b`, `647fd07`)

### Added
- Added `TimerExpiredReceiver` to handle scheduled timer expiration broadcasts.

### Fixed
- Fixed the startup view behavior.
- Reduced blinking/rebinding behavior in active timer rows.

## 2026-02-11 - Locked/asleep timer continuation (`3eaf93f`)

### Added
- Added timer end-time tracking so active timers continue counting down while the app is backgrounded, locked, or asleep.
- Added alarm scheduling support through the manifest and timer activity flow.

### Changed
- Updated timer model and adapter behavior to display remaining time based on persisted end times.

## 2026-02-10 - Initial app (`e7218b8`)

### Added
- Created the initial Android Timers app project.
- Added timer creation, start/pause/reset/delete controls, and custom sound selection.
- Added core Java timer model, activity, adapter, layouts, app theme, launcher assets, Gradle wrapper, and Android project configuration.
