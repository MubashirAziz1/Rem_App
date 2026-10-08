package com.quickalarm.notif

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.quickalarm.R
import com.quickalarm.service.QuickAlarmService
import com.quickalarm.ui.ComposerActivity

/**
 * Central place for the persistent "Quick Alarm" notification: channel creation,
 * the ongoing notification itself, and the PendingIntents it uses.
 */
object Notifications {

    const val CHANNEL_ID = "quick_alarm_persistent"
    const val ONGOING_NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_name),
                    // Low importance: persistent, silent, no heads-up. Still shows on lock screen.
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.channel_desc)
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    /** Builds the ongoing, non-dismissible notification that opens the composer. */
    fun buildOngoing(context: Context): Notification {
        ensureChannel(context)

        val openComposer = Intent(context, ComposerActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openComposer,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val hideIntent = Intent(context, QuickAlarmService::class.java).apply {
            action = QuickAlarmService.ACTION_HIDE
        }
        val hidePending = PendingIntent.getService(
            context,
            1,
            hideIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(context.getString(R.string.notif_title))
            .setContentText(context.getString(R.string.notif_text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, context.getString(R.string.action_hide), hidePending)
            .build()
    }
}
