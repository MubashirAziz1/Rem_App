package com.quickalarm.setup

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.quickalarm.notif.Notifications
import com.quickalarm.data.AppPrefs
import com.quickalarm.ui.SettingsIntents

/**
 * Reads the values the app can actually detect at runtime and maps them to a
 * [NotifState] via [SetupState]. Android calls live here so the model stays pure.
 */
object NotificationStatus {

    fun current(context: Context): NotifState {
        // Ensure the channel exists before we read its importance.
        Notifications.ensureChannel(context)
        val permissionGranted = SettingsIntents.hasNotificationPermission(context)
        val appNotificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val channelBlocked = isChannelBlocked(context)
        // The hide flag is stored as "notification_enabled"; hidden means the user turned it off.
        val hiddenByUser = !AppPrefs.isNotificationEnabled(context)
        return SetupState.resolve(permissionGranted, appNotificationsOn, channelBlocked, hiddenByUser)
    }

    private fun isChannelBlocked(context: Context): Boolean {
        val mgr = context.getSystemService(NotificationManager::class.java)
        val channel = mgr.getNotificationChannel(Notifications.CHANNEL_ID) ?: return false
        return channel.importance == NotificationManager.IMPORTANCE_NONE
    }
}
