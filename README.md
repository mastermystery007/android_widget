# One Minute Coach

A widget-first Android coach that turns a meaningful goal into **one useful next action** based on the time, energy, and context the user has right now.

The project is intentionally local-first: no account, backend, internet permission, analytics SDK, or advertising dependency.

## Product experience

1. Create a goal such as “publish my Android app” or “prepare for an exam”.
2. Check in with available time, energy, and current context.
3. Receive exactly one adaptive micro-action.
4. Start a distraction-free timer, mark it complete, or swap it.
5. See streaks, completion patterns, focus minutes, and recent activity.
6. Place the responsive widget on the home screen for Start, Done, and Swap actions.

## Features

### Adaptive coaching

- Scores candidate actions by time fit, energy fit, context, time of day, recent repetition, completion history, and swap history.
- Provides a concise explanation for each recommendation.
- Avoids repeating recently completed or swapped actions.
- Recalculates immediately whenever the user changes a check-in or action library.
- Includes curated starter libraries for work, study, health, creative, home, and personal goals.
- Supports custom user-created micro-actions.

### Goal and action management

- Multiple goals with one protected active focus.
- Goal outcome statement and category.
- Enable, disable, add, or delete individual actions.
- Per-action completion and swap counts.
- Safe goal deletion with automatic activation of the next available goal.

### Focus mode

- Full-screen countdown.
- Pause and resume.
- Add one minute.
- Complete the action or save partial progress.
- Deep-link entry from the widget and notification.

### Progress

- Completed today.
- Current and best streak.
- Total focus minutes.
- Completion rate.
- Seven-day momentum chart.
- Recent session history.
- Shareable plain-text progress summary.

### Home-screen widget

- Responsive Glance layouts for compact, medium, and expanded sizes.
- Goal, next action, duration, streak, daily count, and recommendation reason.
- Start, Done, and Swap without navigating through the app.
- Immediate refresh after app or widget actions.
- In-app widget pin request on supported launchers.

### Reminders and privacy

- User-selected daily reminder time.
- WorkManager rescheduling for local-time delivery.
- Android 13+ notification permission flow.
- Android backup and device transfer support.
- No internet permission.
- No user account, remote API, tracking SDK, or ad SDK.

## Architecture

```text
app/
  data/
    CoachDatabase.kt       Room entities, DAO, database
    CoachRepository.kt     Local data orchestration and preferences
  domain/
    CoachEngine.kt         Recommendation scoring and statistics
  ui/
    CoachViewModel.kt      State, timer, and app actions
    CoachApp.kt            Material 3 Compose experience
  widget/
    CoachWidget.kt         Responsive Glance widget and callbacks
  worker/
    ReminderWorker.kt      Scheduling and notifications
```

### Technology

- Kotlin 2.4
- Jetpack Compose + Material 3
- Jetpack Glance
- Room
- Preferences DataStore
- WorkManager
- Coroutines and Flow
- JUnit
- Android API 23–36

## Build

Use Android Studio Quail or a compatible recent release with JDK 17.

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
gradle :app:lintDebug
```

The CI workflow installs Gradle 8.13 and Android SDK 36 before running tests, lint, and the debug build.

## Widget testing checklist

- Add the widget at compact, medium, and expanded sizes.
- Resize in portrait and landscape launchers.
- Verify Start opens focus mode.
- Verify Done records a session and rotates the recommendation.
- Verify Swap never immediately returns the same action when alternatives exist.
- Verify dark mode and dynamic colours.
- Verify empty state before creating a goal.
- Verify restoration after process death and device restart.
- Verify Android 8, 12, 13, and 15+ behaviour when possible.

## Product roadmap

The codebase is structured for later additions without requiring a backend:

- recurring goal schedules
- action packs and import/export
- calendar-aware availability
- Health Connect integration
- richer widget configuration
- multilingual coaching
- optional on-device model assistance

## License

Apache License 2.0.
