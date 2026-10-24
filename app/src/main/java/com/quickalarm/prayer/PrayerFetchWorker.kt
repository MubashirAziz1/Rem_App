package com.quickalarm.prayer

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.quickalarm.data.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Fetches the day's sunrise for the saved location, stores it, and hands the six wake-up
 * alarms to the Clock.
 *
 * WorkManager guarantees at-least-once execution, so this must be idempotent: re-running for
 * the same day rewrites the same values and re-dispatches the same times. On any failure the
 * previously stored time is deliberately left intact, so a stale value stays visible on the
 * status screen instead of silently disappearing.
 */
class PrayerFetchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Nothing to fetch until the user has chosen a location.
            val location = AppPrefs.getPrayerLocation(applicationContext)
                ?: return@withContext Result.success()

            // Pass the device's date explicitly so the result can't drift onto the API's
            // own idea of "today" (they can differ around midnight and across zones).
            val today = LocalDate.now()

            when (val result = PrayerTimesClient.fetch(location, today)) {
                is PrayerParseResult.Success -> {
                    AppPrefs.setLastPrayerTimes(applicationContext, result.times)
                    // Suspends between hand-offs (see PrayerScheduler) so the Clock can
                    // process each one instead of dropping all but the first.
                    val summary = PrayerScheduler.schedule(applicationContext, result.times)
                    // Record what actually happened so the status screen reports the real
                    // count instead of assuming the alarms exist.
                    AppPrefs.setLastSchedule(
                        applicationContext,
                        AppPrefs.LastSchedule(
                            succeeded = summary.succeeded,
                            total = summary.total,
                            tomorrow = summary.day == com.quickalarm.alarm.ScheduledDay.TOMORROW,
                        ),
                    )
                    Result.success()
                }

                is PrayerParseResult.Failure ->
                    if (result.retryable) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            // A worker must never crash the process; retry transient problems.
            Result.retry()
        }
    }
}
