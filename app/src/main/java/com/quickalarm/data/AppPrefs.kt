package com.quickalarm.data

import android.content.Context
import androidx.core.content.edit

import com.quickalarm.prayer.PrayerLocation
import com.quickalarm.prayer.PrayerTimes
import java.time.LocalDate
import java.time.LocalTime

/** Tiny SharedPreferences wrapper for the persistent-notification enabled flag, the
 * first-launch setup-complete flag, and the saved prayer location + last fetch.
 * Writes are skipped when the value is unchanged. */
object AppPrefs {
    private const val FILE = "quick_alarm_prefs"
    private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_CITY = "prayer_city"
    private const val KEY_COUNTRY = "prayer_country"
    private const val KEY_LATITUDE = "prayer_latitude"
    private const val KEY_LONGITUDE = "prayer_longitude"
    private const val KEY_METHOD = "prayer_method"
    private const val KEY_LAST_DATE = "prayer_last_date"
    private const val KEY_LAST_SUNRISE = "prayer_last_sunrise"
    private const val KEY_LAST_TIMEZONE = "prayer_last_timezone"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Defaults to true so the notification appears after first launch. */
    fun isNotificationEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATION_ENABLED, true)

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        val prefs = prefs(context)
        // Skip the write when the value is unchanged: avoids spurious disk writes and
        // keeps a null-intent service restart side-effect free.
        if (prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true) == enabled) return
        prefs.edit { putBoolean(KEY_NOTIFICATION_ENABLED, enabled) }
    }

    /** Whether the user has finished first-launch setup. Defaults to false. */
    fun isSetupComplete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SETUP_COMPLETE, false)

    fun setSetupComplete(context: Context, complete: Boolean = true) {
        val prefs = prefs(context)
        if (prefs.getBoolean(KEY_SETUP_COMPLETE, false) == complete) return
        prefs.edit { putBoolean(KEY_SETUP_COMPLETE, complete) }
    }

    // --- Prayer location (set once in the location tab, kept until changed) ---

    /**
     * The saved location, or null when the user hasn't chosen one yet. Returns null unless a
     * complete choice is stored: either a city + country, or a full coordinate pair. The
     * partial cases can only arise from a half-written edit, and scheduling from them would
     * be wrong, so they are treated as "not set".
     */
    fun getPrayerLocation(context: Context): PrayerLocation? {
        val prefs = prefs(context)
        val method = prefs.getInt(KEY_METHOD, 0)
        if (method == 0) return null

        val latitude = prefs.getFloat(KEY_LATITUDE, Float.NaN).takeIf { !it.isNaN() }?.toDouble()
        val longitude = prefs.getFloat(KEY_LONGITUDE, Float.NaN).takeIf { !it.isNaN() }?.toDouble()
        if (latitude != null && longitude != null) {
            return PrayerLocation(null, null, latitude, longitude, method)
        }

        val city = prefs.getString(KEY_CITY, null)?.takeIf { it.isNotBlank() } ?: return null
        val country = prefs.getString(KEY_COUNTRY, null)?.takeIf { it.isNotBlank() } ?: return null
        return PrayerLocation(city, country, null, null, method)
    }

    /** Saves the location. Clears whichever of city/coordinates is not being used. */
    fun setPrayerLocation(context: Context, location: PrayerLocation) {
        prefs(context).edit {
            putInt(KEY_METHOD, location.method)
            if (location.latitude != null && location.longitude != null) {
                putFloat(KEY_LATITUDE, location.latitude.toFloat())
                putFloat(KEY_LONGITUDE, location.longitude.toFloat())
                remove(KEY_CITY)
                remove(KEY_COUNTRY)
            } else {
                putString(KEY_CITY, location.city)
                putString(KEY_COUNTRY, location.country)
                remove(KEY_LATITUDE)
                remove(KEY_LONGITUDE)
            }
        }
    }

    // --- Last successful fetch (kept on failure so a stale value stays visible) ---

    /** The last successfully fetched prayer time, or null if there has never been one. */
    fun getLastPrayerTimes(context: Context): PrayerTimes? {
        val prefs = prefs(context)
        val date = prefs.getString(KEY_LAST_DATE, null) ?: return null
        val sunrise = prefs.getString(KEY_LAST_SUNRISE, null) ?: return null
        return try {
            PrayerTimes(
                date = LocalDate.parse(date),
                sunrise = LocalTime.parse(sunrise),
                timezone = prefs.getString(KEY_LAST_TIMEZONE, null),
            )
        } catch (_: Exception) {
            // A corrupted record is treated as absent rather than crashing the screen.
            null
        }
    }

    fun setLastPrayerTimes(context: Context, times: PrayerTimes) {
        prefs(context).edit {
            putString(KEY_LAST_DATE, times.date.toString())
            putString(KEY_LAST_SUNRISE, times.sunrise.toString())
            putString(KEY_LAST_TIMEZONE, times.timezone)
        }
    }
}
