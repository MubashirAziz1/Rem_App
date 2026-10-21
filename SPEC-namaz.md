# Spec: Daily Sunrise (Fajr deadline) Fetch + Gradual Wake-Up Alarms

Status: **Draft — awaiting user approval. No code has been written.**

This spec adds a second capability to the existing Quick Alarm app. It is a companion to
`SPEC.md` (the lock-screen notification feature, Tasks 1-10 complete). Read that first for
context; this file covers only the new capability.

## Objective

**Who:** the same single user, who already uses Quick Alarm as a fast titled-alarm shortcut.

**What:** wake the user up *gradually* before the Fajr deadline.

- Fetch the day's **Sunrise** time for a user-chosen city/location from a prayer-times API.
- **Sunrise is the anchor** (approved): sunrise is the Fajr deadline, so it doubles as the
  "qaza" cutoff.
- Schedule **six alarms**, the first **30 minutes before** sunrise and then every
  **5 minutes**: `sunrise-30, -25, -20, -15, -10, -5`. All six fire *before* the deadline.
- Refresh the time **once per day** (sunrise drifts daily, and the app may never be opened).
- The user sets the city/location **once**, in a settings tab, and it persists until changed.

**Why the existing app:** it already owns the notification, the composer, and the Clock
hand-off. The new capability reuses the hand-off rather than inventing an alarm engine.

## Decisions log (from the user, this session)

| # | Decision | Value |
|---|---|---|
| N1 | Anchor time | **Sunrise** (Fajr deadline / "qaza" cutoff). Not Fajr adhan. |
| N2 | Alarm mechanism | **Hand off 6 alarms to the stock Clock** (`ACTION_SET_ALARM`). No own alarm engine, no ring screen, no audio. |
| N3 | Location source | **Manual city/location in an in-app tab.** No runtime location permission. Persists until the user changes it. |
| N4 | Refresh trigger | **WorkManager** daily worker. |
| N5 | Alarm count / spacing | 6 alarms, first at `-30 min`, then `+5 min` each. |

## Assumptions

1. Extend the existing app `com.quickalarm`. No rewrite.
2. The stock Clock accepts six back-to-back `ACTION_SET_ALARM` dispatches. If a device
   de-duplicates same-minute alarms, the titles differ, so they should survive. Verify on
   the A72.
3. `ACTION_SET_ALARM` carries **no date** — only hour/minute. It always schedules the *next*
   occurrence of that time. Alarms whose time has already passed today therefore land
   **tomorrow**. This is acceptable for a daily wake-up but must be handled explicitly (see
   Behavior, step 5) and documented as a limitation.
4. The API is a third-party dependency that can be slow, rate-limited, or wrong. Its response
   is **untrusted input** and is validated before use.
5. The A72 has working internet. Offline days keep the previous day's time (see Behavior, step 6).

## API contract (verified this session)

Aladhan, no API key, JSON:

```
GET https://api.aladhan.com/v1/timingsByCity?city={city}&country={country}&method={method}
GET https://api.aladhan.com/v1/timings/{DD-MM-YYYY}?latitude={lat}&longitude={lng}&method={method}
```

Response (relevant slice):

```json
{
  "code": 200,
  "status": "OK",
  "data": {
    "timings": { "Fajr": "04:05", "Sunrise": "05:54", "Dhuhr": "13:04" },
    "date": { "gregorian": { "date": "20-08-2026" } },
    "meta": { "timezone": "Europe/London", "method": { "id": 2 } }
  }
}
```

- `data.timings.Sunrise` is `"HH:mm"` (24-hour). This is the only field we need.
- `method` selects the calculation authority (1 Karachi, 2 ISNA, 3 MWL, 4 Umm Al-Qura, 5 Egyptian,
  13 Diyanet, ...). The user picks it; there is no universally correct value.

## Behavior

### Location tab (new)

A settings screen reachable from the status screen. Contains:

1. **City** and **Country** text fields (the primary path), or **Latitude/Longitude** for a
   manual coordinate override.
2. **Calculation method** picker (the methods above, with their region hints).
3. A "Save" action that persists the choice and immediately triggers a fetch + reschedule.
4. The current fetched sunrise time and its date, so the user can see what is scheduled.

Validation at this boundary: non-blank city, country; if coordinates are used, lat in
`-90..90` and lng in `-180..180`; method must be a known id. Invalid input is rejected with an
inline message and nothing is persisted.

### Daily refresh (WorkManager)

- One **periodic** worker, 24-hour interval, constraint `NetworkType.CONNECTED`.
- The worker: reads the saved location → calls Aladhan → parses and validates `Sunrise` →
  persists `{date, sunriseTime}` → hands the six alarms to the Clock.
- The worker is idempotent: re-running it for the same day rewrites the same values and
  re-dispatches the same six times. WorkManager is at-least-once, so this must be safe.
- The worker is also enqueued as a one-time job on: app launch, location change, and boot
  (WorkManager already re-arms itself on boot).

### Scheduling the six alarms

Given `sunrise` (a `LocalTime`) and `now`:

1. Compute offsets `[-30, -25, -20, -15, -10, -5]` minutes → six `LocalTime`s.
2. For each, hand off via `SetAlarmHandoff` with a distinct title (`"Fajr 1/6"` ... `"Fajr 6/6"`).
3. The Clock's own "next occurrence" rule (`dayFor`) decides today vs tomorrow per alarm.
4. **Skip alarms already passed today**: if `sunrise-30` is in the past, do not dispatch it
   (it would silently land tomorrow and ring a day late). Only dispatch offsets still in the
   future, and note the count.
5. Record the dispatch result (how many landed, and any `NoClockApp`/`Failed`) so the status
   screen can show it honestly.

### Offline / failure

- Fetch failure (no network, non-200, malformed JSON, missing `Sunrise`) **keeps the previous
  stored time** and does not clear it. The worker returns `Result.retry()` for transient
  failures, bounded by WorkManager's backoff.
- The status screen shows the stored time's date, so a stale value is visible rather than silent.
- A parse failure is never allowed to throw into the worker: the parser returns a result type.

## Impact on the approved `SPEC.md` (these clauses must change)

This feature cannot be built without revising the existing spec. The exact clauses:

| Clause | Today | After |
|---|---|---|
| `SPEC.md` **SC-1** | permissions are *exactly* the five | a larger set (see below); `minSdk` 33 unchanged |
| `SPEC.md` **Assumption 3** | "Alarm creation stays a hand-off to the stock Clock" | still true, but now **six** hand-offs, driven by a fetch |
| `SPEC.md` **D4 / Service efficiency** | "no network... in the service" | the *service* is untouched; the **worker** does network I/O. The service stays idle. |
| `SPEC.md` **Boundaries / Never** | "Never request... location... permission" | **held** — no location permission is added |
| `SPEC.md` **Boundaries / Ask first** | "Adding any dependency" | WorkManager is added (this spec is the ask) |
| `scripts/check-permissions.ps1` | asserts the five | must assert the new expected set |

**Permission set after this feature** (verified: WorkManager merges in `ACCESS_NETWORK_STATE`
and `WAKE_LOCK`):

- `com.android.alarm.permission.SET_ALARM` (existing)
- `android.permission.POST_NOTIFICATIONS` (existing)
- `android.permission.FOREGROUND_SERVICE` (existing)
- `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` (existing)
- `android.permission.RECEIVE_BOOT_COMPLETED` (existing)
- **`android.permission.INTERNET`** (new — the API call)
- **`android.permission.ACCESS_NETWORK_STATE`** (new — injected by WorkManager)
- **`android.permission.WAKE_LOCK`** (new — injected by WorkManager)

No `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` (we don't schedule our own alarms), and **no
location permission**.

## Tech Stack

- Existing: Kotlin 2.3.21, Compose (BOM 2026.08.00), Material 3, `minSdk` 33, `targetSdk` 36.
- **New dependency:** `androidx.work:work-runtime-ktx` (only new runtime dependency).
- HTTP: `java.net.HttpURLConnection` (no OkHttp/Retrofit — keeps the dependency count at one).
- JSON: `org.json.JSONObject` (already on the platform, no dependency).
- Time: `java.time.LocalTime` / `LocalDate` (already used by `SetAlarmHandoff`).

## Commands

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\scripts\check-permissions.ps1     # updated for the new permission set
```

## Project Structure (new files)

```
app/src/main/java/com/quickalarm/
  prayer/PrayerTimes.kt          -> NEW: model (date + sunrise LocalTime)
  prayer/PrayerTimesParser.kt    -> NEW: pure JSON -> Result; JVM-testable
  prayer/PrayerTimesClient.kt    -> NEW: HttpURLConnection call (Android edge)
  prayer/PrayerSchedule.kt       -> NEW: pure offset math -> 6 LocalTimes; JVM-testable
  prayer/PrayerFetchWorker.kt    -> NEW: WorkManager worker (fetch -> store -> schedule)
  prayer/PrayerScheduler.kt      -> NEW: dispatches the six Clock hand-offs
  prayer/PrayerLocation.kt       -> NEW: pure location/method validation; JVM-testable
  data/AppPrefs.kt               -> add location, method, and last-fetch keys
  ui/LocationScreen.kt           -> NEW: the location/method tab
  ui/StatusScreen.kt             -> add entry point + "next wake-up" line
app/src/main/AndroidManifest.xml -> add INTERNET
app/build.gradle.kts             -> add work-runtime-ktx
scripts/check-permissions.ps1    -> update the expected permission set
app/src/test/...                 -> NEW tests for the pure pieces
```

## Code Style

Match the existing code: small `object`s for stateless logic, sealed interfaces for results,
short KDoc stating the non-obvious *why*, no `@Suppress` or lint-disable.

```kotlin
/** The six wake-up times for one day, derived from sunrise. Pure, so it is JVM-testable. */
object PrayerSchedule {
    /** Minutes before sunrise, in firing order. */
    val OFFSETS_MIN = listOf(-30, -25, -20, -15, -10, -5)

    /** Returns sunrise + each offset, dropping any time already at or before [now]. */
    fun times(sunrise: LocalTime, now: LocalTime): List<LocalTime> =
        OFFSETS_MIN
            .map { sunrise.plusMinutes(it.toLong()) }
            .filter { it.isAfter(now) }
}
```

## Testing Strategy

- **JVM unit tests** (JUnit 4, `app/src/test`) for every pure piece:
  - `PrayerSchedule.times`: all six offsets, midnight wraparound (sunrise 00:20 -> `-30` wraps
    to the previous day), and the "already passed" filter.
  - `PrayerTimesParser`: a real Aladhan response fixture, a missing-`Sunrise` response, a
    non-200 body, malformed JSON, and an out-of-range time like `"25:99"`.
  - `PrayerLocation`: blank city, coordinate ranges, unknown method id.
- **Manifest check** (scripted): the new permission set, `minSdk` 33, no location permission,
  no exact-alarm permission.
- **On-device** (user, later): the six alarms appear in Clock with the right times; the daily
  worker runs; the location tab persists.

## Boundaries

- **Always:**
  - Keep the six alarms a **Clock hand-off** (N2). No own alarm engine, no audio, no ring screen.
  - Treat the API response as untrusted: validate `Sunrise` before storing or using it.
  - Keep the pure logic (offsets, parsing, validation) free of Android calls so it is testable.
  - Keep the **foreground service idle** — all network work lives in the worker.
- **Ask first:**
  - Any dependency beyond `work-runtime-ktx`.
  - Any permission beyond the eight listed above.
  - Switching the anchor away from sunrise, or changing the count/spacing.
- **Never:**
  - Request a location permission, or read location without one (N3 is manual city).
  - Add background work to `QuickAlarmService`.
  - Let a network or parse failure throw into the worker, or clear the stored time on failure.
  - Silently schedule an alarm whose time has already passed today.

## Success Criteria

- **SC-20** The location tab persists a city + country (or coordinates) + calculation method,
  and re-fetches immediately on save.
- **SC-21** The API response is validated; a malformed, non-200, or `Sunrise`-less response
  never crashes and never overwrites a good stored time.
- **SC-22** `PrayerSchedule` produces exactly six times at `-30, -25, -20, -15, -10, -5` from
  sunrise, and drops times already passed.
- **SC-23** Six alarms are handed to the Clock with distinct titles, each landing at the right
  minute (verified on the A72).
- **SC-24** The daily worker fetches once per day and reschedules; a re-run for the same day is
  idempotent.
- **SC-25** The manifest contains exactly the eight permissions above: **no location
  permission and no exact-alarm permission**. `minSdk` stays 33.
- **SC-26** `QuickAlarmService` still does no network or background work (D4 preserved).
- **SC-27** `assembleDebug`, `lintDebug`, and `testDebugUnitTest` pass with no new suppressions.
- **SC-28 (revises SC-1)** `scripts/check-permissions.ps1` asserts the new eight-permission set
  and still rejects `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

## Risks

| Risk | Impact | Mitigation |
|---|---|---|
| Clock de-duplicates the six alarms | High — fewer than six fire | Distinct titles; verify on the A72 (SC-23). Fall back to a single alarm + five notifications if it fails. |
| `ACTION_SET_ALARM` has no date | Medium — passed alarms land tomorrow | Skip already-passed offsets (step 4); document as a limitation. |
| WorkManager adds `WAKE_LOCK`/`ACCESS_NETWORK_STATE` | Low — widens the permission set | Accepted and recorded above; script updated. `WAKE_LOCK` is unused at runtime on API 33+ (WorkManager only uses it pre-API 23). |
| Periodic work's 24h window drifts | Medium — fetch may land after sunrise | Use `PeriodicWorkRequest` with a 24h interval plus an exact one-time fetch each launch; verify on device. |
| Aladhan is down or changes shape | Medium | Validated parsing, keep last-good time, visible stale date, bounded retry. |
| Six alarms clutter the user's Clock | Low | Distinct titles make them identifiable and easy to delete; document how to remove them. |

## Open Questions

1. **Anchor at sunrise vs Fajr adhan** — approved as sunrise (N1). Confirm this is the intended
   "qaza" cutoff rather than the Fajr adhan time.
2. **The six alarms all ring silently in the Clock.** The Clock app's own sound/volume applies;
   the app cannot set a gentle ramp. Is that acceptable for "wake up gradually", or do you want
   the notification-nudge hybrid instead (alarm only at the anchor)?
3. **WorkManager dependency** — "Ask first" per `SPEC.md`. Approve adding `work-runtime-ktx`?
4. **Timezone — RESOLVED (user, this session): convert.** The API returns the city's local
   time in `data.meta.timezone`. When that differs from the device's zone, the app converts
   the instant into the device's zone before scheduling, so the alarm fires at the correct
   wall-clock moment on the phone. Offsets are applied in the *city's* frame (so "30 minutes
   before sunrise" stays true), then each instant is converted to the device zone. A DST
   transition inside the offset window is handled by working in instants, not wall-clock arithmetic.
