package com.quickalarm.prayer

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.quickalarm.data.AppPrefs
import java.util.concurrent.TimeUnit

/**
 * Registers the prayer-time fetch with WorkManager. Uses fixed unique work names so
 * re-enqueueing updates the existing work instead of stacking duplicate chains.
 */
object PrayerWork {

    private const val PERIODIC_NAME = "prayer_daily_fetch"
    private const val IMMEDIATE_NAME = "prayer_immediate_fetch"

    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * The once-per-day refresh, so sunrise stays current even when the app is never opened.
     * KEEP preserves the existing schedule (and its 24-hour phase) rather than restarting it.
     */
    fun ensureDaily(context: Context) {
        val request = PeriodicWorkRequestBuilder<PrayerFetchWorker>(24, TimeUnit.HOURS)
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    /**
     * An immediate refresh, used on launch and after the location changes. No-ops until a
     * location is saved, so we never fetch with nothing to fetch for.
     */
    fun refreshNow(context: Context) {
        if (AppPrefs.getPrayerLocation(context) == null) return
        val request = OneTimeWorkRequestBuilder<PrayerFetchWorker>()
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
