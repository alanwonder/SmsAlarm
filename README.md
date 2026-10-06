# TP Alert

**TP Alert** (formerly **SmsAlarm**) is an Android app that watches incoming **SMS-style notifications** and plays a loud alarm when a target traffic message is detected.

> Current detection rule: trigger when a new notification from an SMS app contains the text `上海交警`.

## Documentation

- Chinese full project documentation: `docs/项目文档.md`

## What the app does

- Lets you toggle monitoring on/off from a single button in the main screen.
- Uses Android's **Notification Listener** API (not direct SMS read permissions) to inspect posted notification text.
- Extracts text from `title` + `text` + `bigText` + `MessagingStyle` messages, so conversation-style notifications (e.g. Google Messages) don't get missed.
- Filters likely SMS notification sources (`Google Messages`, `AOSP MMS`, or package names containing `sms`).
- Debounces alerts to avoid repeat triggers in a short interval (configurable 1–30 min).
- Relays the alert through an **exact alarm** (`AlarmManager.setAlarmClock`) to a `BroadcastReceiver`, which then starts the alarm service — this is the officially exempt path for starting a foreground service from the background on Android 12+, avoiding `ForegroundServiceStartNotAllowedException`.
- Starts a foreground alarm service that:
  - raises the **alarm stream** volume to max and restores it to the previous level when the alarm stops (music stream is never touched),
  - requests audio focus,
  - loops `res/raw/alarm.mp3`,
  - shows an ongoing notification with a **Stop Alarm** action,
  - auto-stops after a configurable duration (1–30 min).
- Runs a low-priority foreground keep-alive service while monitoring is enabled.
- Guides you to grant notification access and `POST_NOTIFICATIONS` (including a fallback to system notification settings when the permission is permanently denied).

## Tech stack

- Kotlin + Android SDK
- minSdk 29 / targetSdk 34 / compileSdk 34
- AndroidX Core + AppCompat
- Gradle (Groovy DSL, wrapper 8.7)

## Project structure

- `app/src/main/java/com/example/smsalarm/MainActivity.kt`  
  UI entry point, permission prompts, monitoring toggle, debounce/ring-duration settings.
- `app/src/main/java/com/example/smsalarm/SmsNotificationListener.kt`  
  Notification listener, message matching, debounce, exact-alarm scheduling.
- `app/src/main/java/com/example/smsalarm/AlarmTriggerReceiver.kt`  
  Receives the exact alarm and starts `AlarmService` (background FGS exemption path).
- `app/src/main/java/com/example/smsalarm/AlarmService.kt`  
  Foreground alarm playback, volume save/restore, vibration, stop behavior.
- `app/src/main/java/com/example/smsalarm/KeepAliveService.kt`  
  Foreground keep-alive notification/service.
- `app/src/main/java/com/example/smsalarm/MonitorConfig.kt`  
  SharedPreferences-backed monitor/config state.
- `app/src/main/AndroidManifest.xml`  
  Permissions, services, receiver, listener declaration.

## Permissions and system settings

The app requests / depends on:

- `POST_NOTIFICATIONS`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `FOREGROUND_SERVICE_DATA_SYNC`
- `MODIFY_AUDIO_SETTINGS`
- `WAKE_LOCK`
- `VIBRATE`

It also requires manually enabling:

- **Notification access** for this app (`Settings > Notification access`) so `SmsNotificationListener` can receive notification events.

No SMS read/receive permission is used.

## How alert triggering works

1. Monitoring must be enabled in the app (and notification access granted).
2. A new notification is posted; the listener receives it.
3. Notification package must look like an SMS app.
4. Notification text (title/text/bigText/MessagingStyle merged) must contain `上海交警`.
5. Debounce window check passes.
6. An exact alarm (`setAlarmClock`) is scheduled ~1s ahead.
7. The alarm fires `AlarmTriggerReceiver` — an exempt context — which starts `AlarmService` as a foreground service; the alarm plays and auto-stops after the configured duration.

## CI/CD

The workflow (`.github/workflows/android.yml`) builds a **signed release APK** on:

- push to `master`
- tags matching `v*`
- pull requests to `master`
- manual `workflow_dispatch`

Highlights:

- Uses the runner's preinstalled Android SDK (the previous `android-actions/setup-android@v3` action is broken on current runners — it tries to install the removed legacy `tools` package).
- Requires repo secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Release signing is conditional: if the keystore/credentials are missing, the release APK is built unsigned instead of failing the build.
- Each run uploads the APK as an artifact named `app-release-2.0.<git-commit-count>`.

## Build and run

### Prerequisites

- Android Studio (Hedgehog or newer recommended)
- Android SDK 34
- JDK 17 (required by AGP 8.x)

### Debug build

```bash
./gradlew :app:assembleDebug
```

Install from Android Studio or with ADB (filename carries the build version):

```bash
adb install -r app/build/outputs/apk/debug/TP-Alert-2.0.<versionCode>.apk
```

### Release signing

`app/build.gradle` expects a keystore at `../smsalarm.keystore` and reads credentials from environment variables:

- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

Signing is only enabled when the keystore file exists and all three variables are set; otherwise the release APK is built unsigned.

Then build:

```bash
./gradlew :app:assembleRelease
```

## Versioning and artifact naming

- `versionCode` is derived from the Git commit count (`git rev-list --count HEAD`); if Git history is unavailable (e.g. a ZIP download or shallow clone), it falls back to a build-time value so the build never fails.
- `versionName` is `2.0`.
- The APK filename is always `TP-Alert-2.0.<versionCode>.apk`, so every build produces a uniquely named artifact.
- CI artifact name: `app-release-2.0.<git-commit-count>`.

## Current limitations

- Keyword (`上海交警`) and SMS package filtering are hardcoded.
- Audio focus loss (e.g. an incoming call during an alarm) is not yet handled.
- UI is minimal (single screen).

## Support / Donate

If you find this project useful, consider buying me a coffee:

- WeChat Pay 赞赏码: `<!-- TODO: 替换为赞赏码图片，如 docs/donate-wechat.png -->`
- Alipay 收款码: `<!-- TODO: 替换为收款码图片，如 docs/donate-alipay.png -->`
- 爱发电 (Afdian): `<!-- TODO: https://afdian.com/a/<your-id> -->`
- Buy Me a Coffee: `<!-- TODO: https://buymeacoffee.com/<your-id> -->`

## Security and privacy notes

- The app inspects notification text delivered by Android's notification listener system.
- It does not currently upload message content anywhere in this project.
- Evaluate your local privacy/compliance requirements before production deployment.
