package com.quickalarm.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Shared edge helpers for the UI: notification-permission check and the system-settings
 * intents the setup and status screens open. None of these request a runtime permission
 * other than POST_NOTIFICATIONS; opening the battery-optimization *list* needs no
 * permission (only requesting ignore would, which we don't do).
 */
object SettingsIntents {

    fun hasNotificationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    fun shouldShowRationale(context: Context): Boolean {
        // minSdk is 33 (TIRAMISU), so POST_NOTIFICATIONS rationale always applies.
        val activity = context as? android.app.Activity ?: return false
        return activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun appDetails(context: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    )

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        safeStart(context, intent, fallback = appDetails(context))
    }

    /** Security settings, where One UI's lock-screen content controls live. */
    fun openLockScreenSettings(context: Context) {
        safeStart(context, Intent(Settings.ACTION_SECURITY_SETTINGS), fallback = appDetails(context))
    }

    /** The battery-optimization *list* (no permission needed to view). */
    fun openBatteryOptimizationSettings(context: Context) {
        safeStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), fallback = appDetails(context))
    }

    private fun safeStart(context: Context, intent: Intent, fallback: Intent? = null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Throwable) {
            fallback?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(it)
                } catch (_: Throwable) {
                    // ignore
                }
            }
        }
    }
}
