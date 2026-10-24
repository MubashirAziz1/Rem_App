# Quick Alarm - build & run

Android-only "create shell" over the stock Clock. A persistent lock-screen
notification opens a composer (24-hour time + required title) over the keyguard and
hands the alarm to the stock Clock via `ACTION_SET_ALARM` + `EXTRA_SKIP_UI`.

This build trims the app to the approved spec (`SPEC.md`): a minimal first-launch
setup (notification permission + one informational lock-screen check), a status
screen for every later launch, a permission-audited manifest on `minSdk` 33, a
cheaper always-on foreground service, and strict 24-hour alarm input.

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

Debug APK: `app\build\outputs\apk\debug\app-debug.apk`

### Automated gate (no phone needed)

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\scripts\check-permissions.ps1     # SC-1: permission set + minSdk
```

All four pass on this machine. `lintDebug` reports 15 findings, all pre-existing
dependency/version/config nags; none are in the changed logic and no `@Suppress` or
lint-disable was added (SC-19).

## Baseline (before)

Captured from the unchanged app before any code edits (Task 1). The user can install
this APK in Task 11 for a memory/wake-up comparison.

- Baseline APK (git-ignored): `build-baseline\app-baseline.apk`
- Baseline APK size: **18,559,071 bytes (~18,124 KB / ~17.7 MB)**
- Baseline declared permissions (`aapt2 dump permissions`):
  - `com.android.alarm.permission.SET_ALARM`
  - `android.permission.POST_NOTIFICATIONS`
  - `android.permission.FOREGROUND_SERVICE`
  - `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
  - `android.permission.RECEIVE_BOOT_COMPLETED`
  - `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`  (removed after Task 3)
  - `com.quickalarm.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`  (AGP-injected; internal, not in SC-1)

Memory/wake-up baselines (PSS, batterystats, dumpsys alarm/jobs/power) are taken by
the user in Task 11 using this baseline APK, then compared to the post-change APK.

## What changed (build summary)

### Feature 1: lock-screen notification + minimal setup (Tasks 1-10)

- `minSdk` 26 -> 33; obsolete `Build.VERSION.SDK_INT` checks removed from touched files.
- Manifest: `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` removed; `LOCKED_BOOT_COMPLETED`
  filter dropped from `BootReceiver`; `<queries>` added for `ACTION_SET_ALARM` (R3).
- Foreground service passes `FOREGROUND_SERVICE_TYPE_SPECIAL_USE` only on API 34+
  (R5); on API 33 no type is passed.
- Service does no work after posting (no threads/timers/handlers/wake locks/alarms/
  jobs); a null-intent restart re-posts without writing preferences.
- `AppPrefs` setters skip unchanged writes; `setup_complete` preference added.
- `SetAlarmHandoff` validates hour (0..23), minute (0..59), and title (trimmed,
  non-blank) before dispatching; confirmation time is locale-independent `HH:mm`.
- New pure, JVM-tested model: `setup/SetupState.kt` (`NotifState`, `resolve`,
  `route`) and `setup/NotificationStatus.kt` (reads detectable values).
- `MainActivity` reworked to route: first launch -> setup flow (`SetupScreen`:
  welcome -> notification permission -> lock-screen check -> done); later launches
  -> `StatusScreen` (Active / Hidden by you / Notifications blocked, Hide/Show,
  Fix in settings, Test alarm, collapsed troubleshooting). The old four-item
  Samsung checklist is gone from first launch; battery and lock-screen help live
  only in the optional troubleshooting section. No overlay or battery permission is
  ever requested.

### Feature 2: daily sunrise fetch + gradual wake-up alarms (Tasks 20-30)

Spec: `SPEC-namaz.md`. Plan: `tasks/plan-namaz.md`. Tasks: `tasks/todo-namaz.md`.

What it does: fetches the day's **sunrise** (the Fajr deadline / "qaza" cutoff) for a
city the user chooses once, then hands **six alarms** to the stock Clock at
`sunrise-30, -25, -20, -15, -10, -5` minutes.

- **New dependency:** `androidx.work:work-runtime-ktx:2.12.0` (daily refresh).
- **New test-only dependency:** `org.json:json:20260814` (the `android.jar` `org.json` is a
  stub in JVM unit tests; this is not shipped in the APK).
- **New permission:** `android.permission.INTERNET`. WorkManager also merges in
  `ACCESS_NETWORK_STATE` and `WAKE_LOCK`.
- **No location permission and no exact-alarm permission.** The city is user input, and the
  six alarms are a Clock hand-off, so `AlarmManager` is never used. The permission script
  fails if either ever appears.
- **New `prayer/` package.** Pure, JVM-tested logic (`PrayerTimesParser`, `PrayerSchedule`,
  `PrayerLocationValidator`) with Android calls only at the edges (`PrayerTimesClient`,
  `PrayerScheduler`, `PrayerFetchWorker`, `PrayerWork`).
- **Timezone: convert.** The API returns the city's local time. Offsets are applied in the
  *city's* frame, then each instant is converted to the *device's* zone, so "30 minutes before
  sunrise" stays true and a DST transition inside the window still gives exact 5-minute gaps.
- **Daily refresh.** A 24-hour periodic `WorkManager` job (network-constrained) plus an
  immediate one-time fetch on launch and on location save. Re-running the same day is
  idempotent.
- **Failure handling.** A bad or offline fetch keeps the last-good time, marks it stale on the
  status screen, and retries only when the failure is transient (transport, 5xx, 429).
- **New UI.** A location tab (`LocationScreen`) with city+country or coordinates, a
  calculation-method picker, and the fetched sunrise with its date. The status screen gains a
  "Gradual wake-up" card that opens it.

**Merged permission set (SC-25/SC-28), observed with `aapt2 dump permissions`:**

```
com.android.alarm.permission.SET_ALARM
android.permission.POST_NOTIFICATIONS
android.permission.FOREGROUND_SERVICE
android.permission.FOREGROUND_SERVICE_SPECIAL_USE
android.permission.RECEIVE_BOOT_COMPLETED
android.permission.INTERNET                        <- new
android.permission.WAKE_LOCK                       <- injected by androidx.work
android.permission.ACCESS_NETWORK_STATE            <- injected by androidx.work
com.quickalarm.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION   <- AGP-injected, internal
```

**Automated gate (all pass on this machine):** `assembleDebug`, `testDebugUnitTest`
(**54 tests, 0 failures**), `lintDebug` (**12 findings, down from the 15 pre-existing
baseline, none in new or changed code**), and `check-permissions.ps1`.

## Device verification (Task 11 - run this yourself)

The phone is not connected during the build. After the build is finished, connect
the A72 by USB, enable USB debugging, and follow this checklist in order. Each item
names the SC it proves. Follow the Device Safety Protocol in `tasks/plan.md` (USB
only, debug-signed APK only, revoke authorizations when finished, redact device
serials/accounts before pasting anything).

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```

| # | Step | Command / action | Expected | SC |
|---|------|------------------|----------|----|
| 1 | Android version | Settings > About phone | Android 13+ (else install fails) | -/Q1 |
| 2 | Install | `& $adb install -r app\build\outputs\apk\debug\app-debug.apk` | Installs (minSdk 33) | SC-1 |
| 3 | Permission set | `& $adb shell dumpsys package com.quickalarm \| Select-String "permission"` | Exactly the five; no battery perm | SC-1 |
| 4 | Reset to first launch | `& $adb shell pm clear com.quickalarm` | OK | - |
| 5 | Launch | `& $adb shell monkey -p com.quickalarm 1` | Welcome screen | SC-2 |
| 6 | First permission dialog | Continue, then observe | Exactly one POST_NOTIFICATIONS dialog | SC-2 |
| 7 | Grant | Tap "Allow" | Notification appears; advances to lock-screen step | SC-3 |
| 8 | Single deny | pm clear, relaunch, Continue, deny | "Try again" shown; service not started | SC-4 |
| 9 | Permanent deny | Deny twice (or deny + "Don't ask again") | "Open notification settings" replaces "Try again"; service not started | SC-4 |
| 10 | Re-grant via settings | Open settings, enable notifications, return | Auto-advances from permission step | SC-4 |
| 11 | Lock-screen step | "It works" / "Skip" | Both persist setup-complete; Done screen | SC-3 |
| 12 | Later launch | `& $adb shell monkey -p com.quickalarm 1` | Skips setup; status screen; no overlay/battery prompt | SC-3, SC-5 |
| 13 | Status: Active | Default after grant | "Active" | SC-6 |
| 14 | Status: Hidden | Tap Hide | "Hidden by you"; notification gone; Show restores | SC-6, SC-14 |
| 15 | Status: Blocked | Disable app notifications in system settings, return | "Notifications blocked"; "Fix in settings" | SC-6 |
| 16 | Lock-screen flow | Lock, wake, tap notification | Composer over lock screen, no PIN | SC-7, SC-8 |
| 17 | Time wheels | Set 00:05 and 23:59 | Hours 00-23, minutes 00-59; confirmation HH:mm + right day | SC-9 |
| 18 | Alarm lands | Unlock, open Clock | Alarm with same time + title (Clock's own format) | SC-10 |
| 19 | Blank title | Clear title, tap Set alarm | Inline error; no alarm created | SC-11 |
| 20 | No Clock app | (only if testable) disable/uninstall Clock | "No Clock app" error, no crash | SC-12 |
| 21 | Reboot | Reboot, unlock | Notification returns (unless hidden) | SC-13 |
| 22 | Hide then reboot | Hide, reboot, unlock | Stays gone; Show brings it back | SC-14 |
| 23 | `am kill` | `& $adb shell am kill com.quickalarm` (or swipe away) | Notification returns; no restart loop | SC-18 |
| 24 | Efficiency: no wake locks/alarms/jobs | `& $adb shell dumpsys power \| Select-String com.quickalarm`; `dumpsys alarm`; `dumpsys jobscheduler` | Empty (with notification up, composer closed) | SC-15 |
| 25 | Efficiency: CPU | `& $adb shell dumpsys batterystats com.quickalarm` over 1h idle | Background CPU ~0; explain any non-zero | SC-16 |
| 26 | Efficiency: memory | `& $adb shell dumpsys meminfo com.quickalarm` (total PSS) | Record; no higher than baseline APK | SC-17 |
| 27 | Exported composer (D5/R6) | From adb: `& $adb shell am start -n com.quickalarm/.ui.ComposerActivity` | Only the form shows; creates no alarm by itself | R6 |
| 28 | Cleanup | Disable USB debugging; revoke authorizations; redact before pasting | Done | - |

Record each result (pass/fail + the measured number for 24-26) in the table below,
then paste it back to the agent. The agent will record it here and check off the SC
list, or note waivers. Any failure becomes a new fix task.

### A72 results (filled in by the user)

| # | Result | Notes |
|---|--------|------|
| 1 | | |
| 2 | | |
| 3 | | |
| 4 | | |
| 5 | | |
| 6 | | |
| 7 | | |
| 8 | | |
| 9 | | |
| 10 | | |
| 11 | | |
| 12 | | |
| 13 | | |
| 14 | | |
| 15 | | |
| 16 | | |
| 17 | | |
| 18 | | |
| 19 | | |
| 20 | | |
| 21 | | |
| 22 | | |
| 23 | | |
| 24 | | |
| 25 | | |
| 26 | | |
| 27 | | |
| 28 | | |

## Device verification: sunrise alarms (Task 31 - run this yourself)

Run this after the checklist above, in the same session. Each item names the SC it proves.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r "app\build\outputs\apk\debug\app-debug.apk"
```

| # | Step | Command / action | Expected | SC |
|---|------|------------------|----------|----|
| P1 | Open the location tab | Status screen > "Gradual wake-up" card | Location tab opens | SC-20 |
| P2 | Save a city | Enter city + country, pick a method, "Save and fetch now" | Sunrise time appears with today's date | SC-20 |
| P3 | Fetch actually happened | Check the displayed time against a known source | Times agree | SC-20 |
| P4 | Alarms exist | Open the Clock app | Six alarms, titled `Fajr 1/6` ... `Fajr 6/6` | SC-23 |
| P5 | Alarm times | Inspect each alarm | `sunrise-30, -25, -20, -15, -10, -5` | SC-23 |
| P6 | **De-duplication** | Count the alarms in Clock | **Exactly six.** Note if fewer | SC-23 |
| P7 | Persistence | Force-stop and relaunch | The city and time are still shown | SC-20 |
| P8 | Offline keeps last time | Airplane mode on, relaunch, open the tab | Previous time kept, marked stale; no crash | SC-21 |
| P9 | Permission set | `& $adb shell dumpsys package com.quickalarm \| Select-String "permission"` | The eight above; no location, no exact-alarm | SC-25 |
| P10 | No location prompt | Throughout the flow | No location permission is ever requested | SC-25 |
| P11 | Notification unaffected | Lock screen | The Quick Alarm notification still works as before | SC-26 |
| P12 | Service still idle | `& $adb shell dumpsys alarm \| Select-String com.quickalarm` | Still empty (the fetch is WorkManager, not the service) | SC-26 |
| P13 | Worker ran | `& $adb shell dumpsys jobscheduler \| Select-String com.quickalarm` | WorkManager job present, not the service | SC-24 |
| P14 | Timezone conversion | Set a city in a different timezone than the phone | Alarms land at the correct *local* wall-clock time | SC-23 |
| P15 | Cleanup | Remove the six test alarms from Clock if unwanted | Clock tidied | - |

**The single most important item is P6.** If the Clock de-duplicates the six alarms, the
fallback is one alarm at the anchor plus five notifications. Please report the exact count.

### Sunrise-alarm results (filled in by the user)

| # | Result | Notes |
|---|--------|-------|
| P1 | | |
| P2 | | |
| P3 | | |
| P4 | | |
| P5 | | |
| P6 | | |
| P7 | | |
| P8 | | |
| P9 | | |
| P10 | | |
| P11 | | |
| P12 | | |
| P13 | | |
| P14 | | |
| P15 | | |

## Notes / accepted limitations

- The hand-off to Clock is fire-and-forget: `ACTION_SET_ALARM` returns nothing, so
  "success" means the intent resolved to a Clock app and dispatched without an
  exception, not a guarantee the alarm was persisted. There is no silent fallback
  scheduler by design.
- The persistent notification is backed by a foreground service
  (`FOREGROUND_SERVICE_TYPE_SPECIAL_USE` on API 34+). The notification's "Hide"
  action stops the service (the deliberate anti-malware escape hatch); re-enable it
  from the status screen's "Show notification" button.
- Samsung One UI may still stop the service ("sleeping apps" / Android 13+ Task
  Manager). The app re-arms on next launch or reboot, but not live; that's why the
  optional troubleshooting section exists.

### Sunrise alarms (feature 2)

- **The six alarms ring at the Clock app's own volume.** The app cannot make them ramp
  gently — "wake up gradually" here means six chances at 5-minute intervals, not a soft
  volume build-up. This is inherent to handing off to the stock Clock.
- **The six hand-offs are spaced 2 seconds apart** (`PrayerScheduler.DISPATCH_GAP_MS`).
  `ACTION_SET_ALARM` is an *Activity* action, so each hand-off starts the Clock. Sending all
  six in one burst made the Clock accept the first and silently drop the rest (a known
  behavior of several stock Clock apps); the gap fixes it. Do not collapse this back into a
  tight loop — the spacing is what makes all six land.
- **`ACTION_SET_ALARM` carries no date.** It only takes hour and minute, and the Clock always
  schedules the *next* occurrence. Any offset already passed today is therefore skipped
  rather than dispatched (it would otherwise silently land tomorrow and ring a day late).
- **Six alarms will appear in the Clock app.** They are titled `Fajr 1/6` ... `Fajr 6/6` so
  they are identifiable. Delete them in the Clock app like any other alarm.
- **WorkManager's 24-hour window is not exact.** Android may run periodic work slightly late
  (especially under Doze). The immediate fetch on every app launch and on location save
  compensates, so opening the app always refreshes the time.
- **The API is a third-party dependency.** If Aladhan is down or unreachable, the last-good
  time is kept and shown as stale rather than cleared.
