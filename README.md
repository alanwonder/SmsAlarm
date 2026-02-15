# SmsAlarm

SmsAlarm is an Android app that watches incoming **SMS-style notifications** and plays a loud alarm when a target traffic message is detected.

> Current detection rule: trigger when a new notification from an SMS app contains the text `上海交警`.

## Documentation

- Chinese full project documentation: `docs/项目文档.md`


## What the app does

- Lets you toggle monitoring on/off from a single button in the main screen.
- Uses Android's **Notification Listener** API (not direct SMS read permissions) to inspect posted notification text.
- Filters likely SMS notification sources (`Google Messages`, `AOSP MMS`, or package names containing `sms`).
- Debounces alerts to avoid repeat triggers in a short interval.
- Starts a foreground alarm service that:
  - raises alarm/media volume to max,
  - requests audio focus,
  - loops `res/raw/alarm.mp3`,
  - shows an ongoing notification with a **Stop Alarm** action,
  - auto-stops after 10 minutes.
- Runs a low-priority foreground keep-alive service while monitoring is enabled.

## Tech stack

- Kotlin + Android SDK
- minSdk 29 / targetSdk 34 / compileSdk 34
- AndroidX Core + AppCompat
- Gradle (Groovy DSL)

## Project structure

- `app/src/main/java/com/example/smsalarm/MainActivity.kt`  
  UI entry point, permission prompts, monitoring toggle.
- `app/src/main/java/com/example/smsalarm/SmsNotificationListener.kt`  
  Notification listener and message matching logic.
- `app/src/main/java/com/example/smsalarm/AlarmService.kt`  
  Foreground alarm playback and stop behavior.
- `app/src/main/java/com/example/smsalarm/KeepAliveService.kt`  
  Foreground keep-alive notification/service.
- `app/src/main/java/com/example/smsalarm/MonitorConfig.kt`  
  SharedPreferences-backed monitor enable/disable state.
- `app/src/main/AndroidManifest.xml`  
  Permissions, services, listener declaration.

## Permissions and system settings

The app requests / depends on:

- `POST_NOTIFICATIONS`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `MODIFY_AUDIO_SETTINGS`
- `WAKE_LOCK`

It also requires manually enabling:

- **Notification access** for this app (`Settings > Notification access`) so `SmsNotificationListener` can receive notification events.

## How alert triggering works

1. Monitoring must be enabled in the app.
2. A new notification is posted.
3. Notification package must look like an SMS app.
4. Notification text/body must contain `上海交警`.
5. Debounce window check passes.
6. `AlarmService` starts as a foreground service and plays alarm audio.

## Build and run

### Prerequisites

- Android Studio Hedgehog+ (or compatible)
- Android SDK 34
- JDK 11

### Debug build

```bash
./gradlew :app:assembleDebug
```

Install from Android Studio or with ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release signing

`app/build.gradle` expects a keystore at `../smsalarm.keystore` and reads credentials from environment variables:

- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Then build:

```bash
./gradlew :app:assembleRelease
```

## Versioning

`versionCode` is derived from Git commit count (`git rev-list --count HEAD`).

> Note: shallow clones are rejected; fetch full history before building release/versioned artifacts.

## Current limitations

- Keyword and package filtering are hardcoded.
- Debounce interval and play duration are currently fixed in code.
- UI is minimal (single toggle button).

## Security and privacy notes

- The app inspects notification text delivered by Android's notification listener system.
- It does not currently upload message content anywhere in this project.
- Evaluate your local privacy/compliance requirements before production deployment.
