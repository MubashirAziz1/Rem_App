package com.quickalarm.data

import android.content.Context

/** Tiny SharedPreferences wrapper for the persistent-notification enabled flag and the
 * first-launch setup-complete flag. Writes are skipped when the value is unchanged. */
object AppPrefs {
    private const val FILE = "quick_alarm_prefs"
    private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"
    private const val KEY_SETUP_COMPLETE = "setup_complete"

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
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }

    /** Whether the user has finished first-launch setup. Defaults to false. */
    fun isSetupComplete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SETUP_COMPLETE, false)

    fun setSetupComplete(context: Context, complete: Boolean = true) {
        val prefs = prefs(context)
        if (prefs.getBoolean(KEY_SETUP_COMPLETE, false) == complete) return
        prefs.edit().putBoolean(KEY_SETUP_COMPLETE, complete).apply()
    }
}
