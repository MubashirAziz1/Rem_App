package com.quickalarm.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import com.quickalarm.ui.theme.CheckGlyph
import com.quickalarm.ui.theme.GhostButton
import com.quickalarm.ui.theme.Motion
import com.quickalarm.ui.theme.PrimaryButton
import com.quickalarm.ui.theme.QuickAlarmColors
import com.quickalarm.ui.theme.Reveal
import com.quickalarm.ui.theme.SecondaryButton
import com.quickalarm.ui.theme.Spacing
import com.quickalarm.ui.theme.StepLabel
import com.quickalarm.ui.theme.WordRevealText

/** The linear first-launch flow. Intro -> notification permission -> lock-screen check -> Done. */
private enum class SetupStep { Intro, Permission, LockScreenCheck, Done }

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
    var step by remember { mutableStateOf(SetupStep.Intro) }
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
            .safeDrawingPadding()
            .padding(horizontal = Spacing.lg),
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (
                    slideInHorizontally(tween(Motion.DurationMs, easing = Motion.Fluid)) { it / 4 } +
                        fadeIn(tween(Motion.DurationMs, easing = Motion.Fluid))
                    ) togetherWith (
                    slideOutHorizontally(tween(Motion.DurationMs, easing = Motion.Fluid)) { -it / 4 } +
                        fadeOut(tween(Motion.ExitMs))
                    )
            },
            label = "setupStep",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                SetupStep.Intro -> IntroStep(onContinue = { step = SetupStep.Permission })
                SetupStep.Permission -> PermissionStep(
                    permanentlyDenied = permanentlyDenied,
                    onAllow = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                    onOpenSettings = onOpenNotificationSettings,
                    onTryAgain = {
                        permanentlyDenied = false
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
                SetupStep.LockScreenCheck -> LockScreenStep(
                    onOpenLockScreenSettings = onOpenLockScreenSettings,
                    onFinish = {
                        onSetupComplete()
                        step = SetupStep.Done
                    },
                )
                SetupStep.Done -> DoneStep(onDone = onDone)
            }
        }
    }
}

/** Page 2 of the flow: the problem, in one sentence, revealed word by word. */
@Composable
private fun IntroStep(onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(Spacing.xxxl))
        Reveal(0) {
            Text(
                text = "Quick Alarm",
                style = MaterialTheme.typography.titleMedium,
                color = QuickAlarmColors.TextSecondary,
            )
        }
        Spacer(Modifier.height(Spacing.md))
        WordRevealText(
            text = "Setting an alarm should take seconds, even when you are half asleep.",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(Spacing.lg))
        Reveal(3) {
            Text(
                text = "Quick Alarm keeps a shortcut on your lock screen. Tap it, " +
                    "pick a time, name it, and your Clock app does the rest.",
                style = MaterialTheme.typography.bodyLarge,
                color = QuickAlarmColors.TextSecondary,
            )
        }
        Spacer(Modifier.weight(1f))
        Reveal(4) {
            PrimaryButton(
                text = "Continue",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun PermissionStep(
    permanentlyDenied: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
    onTryAgain: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(Spacing.xxxl))
        Reveal(0) { StepLabel(current = 1, total = 2) }
        Spacer(Modifier.height(Spacing.md))
        Reveal(1) {
            Text(
                text = "Allow notifications",
                style = MaterialTheme.typography.headlineSmall,
                color = QuickAlarmColors.TextPrimary,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Reveal(2) {
            Text(
                text = "The shortcut lives in a notification pinned to your lock " +
                    "screen. Quick Alarm cannot show it without the notification " +
                    "permission, and it asks for nothing else.",
                style = MaterialTheme.typography.bodyLarge,
                color = QuickAlarmColors.TextSecondary,
            )
        }
        Spacer(Modifier.weight(1f))
        if (permanentlyDenied) {
            Reveal(3) {
                PrimaryButton(
                    text = "Open notification settings",
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Reveal(4) {
                SecondaryButton(
                    text = "Try again",
                    onClick = onTryAgain,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Reveal(3) {
                PrimaryButton(
                    text = "Allow notifications",
                    onClick = onAllow,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Reveal(4) {
                Text(
                    text = "Android shows one permission dialog.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = QuickAlarmColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun LockScreenStep(
    onOpenLockScreenSettings: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(Spacing.xxxl))
        Reveal(0) { StepLabel(current = 2, total = 2) }
        Spacer(Modifier.height(Spacing.md))
        Reveal(1) {
            Text(
                text = "Check your lock screen",
                style = MaterialTheme.typography.headlineSmall,
                color = QuickAlarmColors.TextPrimary,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Reveal(2) {
            Text(
                text = "Lock your phone and wake it. You should see the Quick Alarm " +
                    "notification. If it is missing, your lock screen may be hiding " +
                    "notification content. This step is optional.",
                style = MaterialTheme.typography.bodyLarge,
                color = QuickAlarmColors.TextSecondary,
            )
        }
        Spacer(Modifier.weight(1f))
        Reveal(3) {
            SecondaryButton(
                text = "Open lock screen settings",
                onClick = onOpenLockScreenSettings,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Reveal(4) {
            PrimaryButton(
                text = "It works",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Reveal(5) {
            GhostButton(
                text = "Skip for now",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}

@Composable
private fun DoneStep(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(Spacing.xxxl))
        Spacer(Modifier.height(Spacing.xl))
        Reveal(0) {
            CheckGlyph(modifier = Modifier.size(72.dp))
        }
        Spacer(Modifier.height(Spacing.lg))
        Reveal(1) {
            Text(
                text = "You are all set",
                style = MaterialTheme.typography.headlineSmall,
                color = QuickAlarmColors.TextPrimary,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Reveal(2) {
            Text(
                text = "Tap the Quick Alarm notification on your lock screen " +
                    "whenever you need an alarm.",
                style = MaterialTheme.typography.bodyLarge,
                color = QuickAlarmColors.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        Spacer(Modifier.weight(1f))
        Reveal(3) {
            PrimaryButton(
                text = "Done",
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
    }
}
