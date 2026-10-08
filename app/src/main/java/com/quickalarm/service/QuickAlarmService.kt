package com.quickalarm.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.quickalarm.data.AppPrefs
import com.quickalarm.notif.Notifications

/**
 * Foreground service that owns the persistent, non-dismissible "Quick Alarm"
 * notification. The Hide action stops the service (removing the notification),
 * which is the deliberate anti-malware escape hatch.
 */
class QuickAlarmService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HIDE -> {
                AppPrefs.setNotificationEnabled(this, false)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                AppPrefs.setNotificationEnabled(this, true)
                startAsForeground()
            }
        }
        // Restart if the system kills us so the notification comes back.
        return START_STICKY
    }

    private fun startAsForeground() {
        val notification = Notifications.buildOngoing(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                Notifications.ONGOING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(Notifications.ONGOING_NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val ACTION_HIDE = "com.quickalarm.action.HIDE"
        const val ACTION_SHOW = "com.quickalarm.action.SHOW"

        /** Starts the persistent notification (only if the user hasn't hidden it). */
        fun start(context: Context) {
            val intent = Intent(context, QuickAlarmService::class.java).apply {
                action = ACTION_SHOW
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Stops the service and removes the notification. */
        fun hide(context: Context) {
            val intent = Intent(context, QuickAlarmService::class.java).apply {
                action = ACTION_HIDE
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
