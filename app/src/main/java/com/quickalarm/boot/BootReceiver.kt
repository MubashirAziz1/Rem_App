package com.quickalarm.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.quickalarm.data.AppPrefs
import com.quickalarm.service.QuickAlarmService

/**
 * Re-arms the persistent notification after reboot or app update, but only when the
 * user hasn't hidden it and app notifications are still enabled. LOCKED_BOOT_COMPLETED
 * is intentionally not handled: this receiver isn't direct-boot aware, so it would
 * never receive that action anyway.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (AppPrefs.isNotificationEnabled(context) &&
                    NotificationManagerCompat.from(context).areNotificationsEnabled()
                ) {
                    QuickAlarmService.start(context)
                }
            }
        }
    }
}
