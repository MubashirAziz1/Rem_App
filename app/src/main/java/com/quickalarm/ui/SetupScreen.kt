package com.quickalarm.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** The linear first-launch flow. Welcome -> notification permission -> lock-screen check -> Done. */
private enum class SetupStep { Welcome, Permission, LockScreenCheck, Done }

/**
 * The first-launch setup flow. Hosts the permission launcher and advances through the
 * steps. The service is started only after the permission is granted; a denial never
 * starts the service or loops the prompt, and a permanent denial swaps "Try again" for
 * "Open notification settings". Returning from system settings with the permission
 * granted auto-advances on resume.
 */
@Composable
fun SetupScreen(
    onPermissionGranted: () -> Unit,
    onSetupComplete: () -> Unit,
    onDone: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenLockScreenSettings: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(SetupStep.Welcome) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onPermissionGranted()
            step = SetupStep.LockScreenCheck
        } else {
            permanentlyDenied = !SettingsIntents.shouldShowRationale(context)
        }
    }

    // If the user grants the permission outside the app (system settings), advance on resume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(step) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                step == SetupStep.Permission &&
                SettingsIntents.hasNotificationPermission(context)
            ) {
                onPermissionGranted()
                step = SetupStep.LockScreenCheck
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        when (step) {
            SetupStep.Welcome -> {
                Title("Quick Alarm")
                Spacer(Modifier.height(12.dp))
                Body(
                    "Set alarms from your lock screen. Quick Alarm adds a shortcut " +
                        "notification so you can tap, pick a time, type a title, and hand " +
                        "the alarm to your Clock app.",
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { step = SetupStep.Permission }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continue")
                }
            }

            SetupStep.Permission -> {
                Title("Allow notifications")
                Spacer(Modifier.height(12.dp))
                Body(
                    "Quick Alarm needs the notification permission so the shortcut can " +
                        "appear on your lock screen. Without it, the shortcut can't show.",
                )
                Spacer(Modifier.height(24.dp))
                if (permanentlyDenied) {
                    OutlinedButton(
                        onClick = onOpenNotificationSettings,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Open notification settings") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { permanentlyDenied = false; launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Try again") }
                } else {
                    Button(
                        onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Allow") }
                }
            }

            SetupStep.LockScreenCheck -> {
                Title("Check your lock screen")
                Spacer(Modifier.height(12.dp))
                Body(
                    "Lock your phone and wake the screen. You should see the " +
                        "\"Quick Alarm\" notification. If it isn't visible, your lock " +
                        "screen may be set to hide content. This step is optional and " +
                        "never blocks setup.",
                )
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onOpenLockScreenSettings,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Open lock screen settings") }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSetupComplete()
                        step = SetupStep.Done
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("It works") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onSetupComplete()
                        step = SetupStep.Done
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Skip") }
            }

            SetupStep.Done -> {
                Title("You're all set")
                Spacer(Modifier.height(12.dp))
                Body(
                    "Tap the Quick Alarm notification on your lock screen to create a " +
                        "titled alarm anytime.",
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}
