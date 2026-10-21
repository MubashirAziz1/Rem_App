package com.quickalarm.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quickalarm.ui.theme.ClockGlyph
import com.quickalarm.ui.theme.HeroGradientText
import com.quickalarm.ui.theme.Motion
import com.quickalarm.ui.theme.QuickAlarmColors
import com.quickalarm.ui.theme.Spacing

/**
 * Minimum time the welcome screen stays up. Covers cold-start settling (first prefs
 * read, service warm-up) and keeps the brand moment from flashing by.
 */
const val WELCOME_MIN_MS = 1700

/**
 * Entry screen shown to every user while the app initializes. Greets with the brand
 * mark and wordmark, shows a loading bar that fills over the init window, then hands
 * off to either the setup flow (first launch) or the status screen.
 */
@Composable
fun WelcomeScreen() {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(durationMillis = WELCOME_MIN_MS, easing = Motion.Fluid),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ClockGlyph(modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(Spacing.xl))
        HeroGradientText(
            text = "Quick Alarm",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = "Alarms from your lock screen",
            style = MaterialTheme.typography.bodyMedium,
            color = QuickAlarmColors.TextSecondary,
        )
        Spacer(Modifier.height(Spacing.xxl))
        // Loading track: fills exactly over the init window.
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(2.dp)
                .background(QuickAlarmColors.SurfaceRaised, CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.value)
                    .height(2.dp)
                    .background(QuickAlarmColors.TextPrimary, CircleShape),
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Getting things ready",
            style = MaterialTheme.typography.bodyMedium,
            color = QuickAlarmColors.TextSecondary.copy(alpha = 0.7f),
        )
    }
}
