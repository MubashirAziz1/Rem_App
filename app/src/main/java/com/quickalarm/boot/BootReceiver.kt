package com.quickalarm.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quickalarm.data.AppPrefs
import com.quickalarm.service.QuickAlarmService

/** Re-arms the persistent notification after reboot or app update, unless hidden. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (AppPrefs.isNotificationEnabled(context)) {
                    QuickAlarmService.start(context)
                }
            }
        }
    }
}
