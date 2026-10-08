package com.quickalarm.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickalarm.data.AppPrefs
import com.quickalarm.service.QuickAlarmService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                val context = this
                var enabled by remember {
                    val prefOn = AppPrefs.isNotificationEnabled(context)
                    mutableStateOf(prefOn && hasNotificationPermission(context))
                }

                val notifPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        QuickAlarmService.start(context)
                        enabled = true
                    }
                }

                fun enableNotification() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        QuickAlarmService.start(context)
                        enabled = true
                    }
                }

                // Auto-arm on launch if the user hasn't hidden it and we can post.
                remember {
                    if (AppPrefs.isNotificationEnabled(context) &&
                        hasNotificationPermission(context)
                    ) {
                        QuickAlarmService.start(context)
                    }
                    true
                }

                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = "Quick Alarm",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "A persistent lock-screen shortcut. Tap the notification, " +
                                "pick a time, type a title, and it hands the alarm off to your Clock app.",
                            style = MaterialTheme.typography.bodyMedium,
                        )

                        Spacer(Modifier.height(20.dp))

                        Card {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = if (enabled) {
                                        "Notification is ON"
                                    } else {
                                        "Notification is OFF"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { enableNotification() },
                                        enabled = !enabled,
                                    ) { Text("Show notification") }
                                    OutlinedButton(
                                        onClick = {
                                            QuickAlarmService.hide(context)
                                            enabled = false
                                        },
                                        enabled = enabled,
                                    ) { Text("Hide") }
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = "Samsung (One UI) setup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "If the notification disappears or the composer misbehaves " +
                                "on the lock screen, check these:",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))

                        SetupItem(
                            title = "1. Allow notifications",
                            desc = "Make sure notifications are enabled for Quick Alarm.",
                            buttonText = "Open notification settings",
                            onClick = { openNotificationSettings(context) },
                        )
                        SetupItem(
                            title = "2. Show content on lock screen",
                            desc = "Set lock-screen notifications to show content so the shortcut is tappable.",
                            buttonText = "Open lock screen settings",
                            onClick = { openLockScreenSettings(context) },
                        )
                        SetupItem(
                            title = "3. Turn off battery optimization",
                            desc = "Set Quick Alarm to Unrestricted so the notification isn't killed.",
                            buttonText = "Battery settings",
                            onClick = { openBatteryOptimization(context) },
                        )
                        SetupItem(
                            title = "4. Appear on top / draw over apps",
                            desc = "If the composer sheet doesn't show over the lock screen, allow this permission.",
                            buttonText = "App details",
                            onClick = { openAppDetails(context) },
                        )

                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupItem(
    title: String,
    desc: String,
    buttonText: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text = desc, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onClick) { Text(buttonText) }
        }
    }
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

private fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

private fun appDetailsIntent(context: Context) = Intent(
    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
    Uri.fromParts("package", context.packageName, null),
)

private fun openAppDetails(context: Context) {
    safeStart(context, appDetailsIntent(context))
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        appDetailsIntent(context)
    }
    safeStart(context, intent, fallback = appDetailsIntent(context))
}

private fun openLockScreenSettings(context: Context) {
    // No universal lock-screen-content intent; land the user on Security settings,
    // falling back to app details.
    val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
    safeStart(context, intent, fallback = appDetailsIntent(context))
}

private fun openBatteryOptimization(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        appDetailsIntent(context)
    }
    safeStart(context, intent, fallback = appDetailsIntent(context))
}
