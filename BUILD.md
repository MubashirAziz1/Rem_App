# Quick Alarm - build & run

Android-only "create shell" over the stock Clock. A persistent lock-screen
notification opens a composer (time + required title) over the keyguard and hands
the alarm off to the stock Clock via `ACTION_SET_ALARM` + `EXTRA_SKIP_UI`.

## Toolchain (already installed on this machine)

- JDK 17 (Temurin): `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`
- Android SDK: `%LOCALAPPDATA%\Android\Sdk`
  - `platform-tools` (adb 37.0.1)
  - `platforms;android-37.0`
  - `build-tools;37.0.0` (AGP also auto-installs `36.0.0`)
- Gradle 9.7.1 (extracted at `C:\Gradle\gradle-9.7.1`; the project wrapper also
  targets 9.7.1)

Environment variables set at user scope: `JAVA_HOME`, `ANDROID_HOME`,
`ANDROID_SDK_ROOT`. The SDK path is also written to `local.properties`
(git-ignored).

## Build

From the project root (`D:\AI\Agentic Coding\Rem_App`):

```powershell
# Using the project wrapper (downloads Gradle 9.7.1 on first run):
.\gradlew.bat assembleDebug

# Or reuse the already-extracted Gradle to skip the download:
& "C:\Gradle\gradle-9.7.1\bin\gradle.bat" assembleDebug
```

Output APK: `app\build\outputs\apk\debug\app-debug.apk`

## Install & run on the Samsung A72

1. On the phone: Settings > About phone > Software information > tap "Build
   number" 7 times to enable Developer options. Then Settings > Developer
   options > enable "USB debugging".
2. Connect the A72 by USB and accept the "Allow USB debugging" prompt.
3. Install and launch:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices                 # confirm the device is listed
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"
& $adb shell monkey -p com.quickalarm 1   # launch the app once
```

4. In the app: tap "Show notification" and grant the notification permission
   (Android 13+). Work through the "Samsung (One UI) setup" checklist:
   - Allow notifications
   - Show content on the lock screen
   - Turn off battery optimization (set Quick Alarm to Unrestricted)
   - Allow "Appear on top" if the composer sheet does not show over lock

## First build spike (definition of "works for me")

On the A72 from a secure lock screen (PIN/pattern set):

1. Lock the phone, wake the screen.
2. The persistent "Quick Alarm" notification is visible on the lock screen.
3. Tap it -> the composer opens over the keyguard without entering the PIN.
4. Pick hour/minute (24h), type a required title.
5. Tap "Set alarm" -> the sheet shows a success confirmation, stating whether the
   alarm is for today or tomorrow (if the time already passed today).
6. Unlock and open the Clock app -> the alarm appears with the correct time and
   title.

If the free-text keyboard is flaky over the lock screen (a known Samsung quirk),
the composer already calls `KeyguardManager.requestDismissKeyguard` to make the
IME usable.

## Notes / accepted limitations

- The hand-off to Clock is fire-and-forget: `ACTION_SET_ALARM` returns nothing, so
  "success" means the intent resolved to a Clock app and dispatched without an
  exception, not a guarantee the alarm was persisted. There is no silent fallback
  scheduler by design.
- The persistent notification is backed by a foreground service
  (`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`). The notification's "Hide" action stops
  the service (the deliberate anti-malware escape hatch); re-enable it from the
  app's "Show notification" button.
