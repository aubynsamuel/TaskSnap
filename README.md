# TaskSnap

TaskSnap is an Android task manager built around the Eisenhower Matrix. It helps you sort tasks by urgency and importance, then capture new tasks without interrupting what you are doing.

## Features

- Organize tasks into four quadrants: Do First, Schedule, Delegate, and Delete
- Create, edit, complete, and delete tasks
- Save tasks locally with Room for offline use
- Set task reminders
- Create tasks from selected or shared text
- Prompt for a follow-up task after a phone call
- View and manage completed tasks

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- Room
- Hilt
- Navigation 3

## Requirements

- Android Studio with Android SDK 36
- JDK 17 or newer
- An Android device or emulator running Android 8.0 (API 26) or newer

## Getting started

1. Clone the repository.
2. Open the project in Android Studio.
3. Let Gradle finish syncing.
4. Select the `app` configuration and run it on a device or emulator.

You can also build a debug APK from the command line:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## Permissions

TaskSnap asks for phone state, call log, contacts, notifications, overlay, exact alarm, and battery optimization permissions. These permissions support post-call task capture and reliable reminders. You can still use the main task manager without enabling every integration.

## Run tests

```bash
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```
