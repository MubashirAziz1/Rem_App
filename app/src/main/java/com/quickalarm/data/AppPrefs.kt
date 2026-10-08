package com.quickalarm.data

import android.content.Context

/** Tiny SharedPreferences wrapper for the persistent-notification enabled flag. */
object AppPrefs {
    private const val FILE = "quick_alarm_prefs"
    private const val KEY_NOTIFICATION_ENABLED = "notification_enabled"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Defaults to true so the notification appears after first launch. */
    fun isNotificationEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIFICATION_ENABLED, true)

    fun setNotificationEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
    }
}
