package com.quickalarm.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared building blocks for every screen. Buttons, entrances, headings, and glyphs
 * all come from here so the visual language stays identical across pages.
 */

/** Full-width primary action. White pill, black label, 16sp semibold. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = QuickAlarmColors.TextPrimary,
            contentColor = QuickAlarmColors.Background,
            disabledContainerColor = QuickAlarmColors.Outline,
            disabledContentColor = QuickAlarmColors.TextSecondary,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Secondary action. Quiet outlined pill. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        shape = CircleShape,
        border = BorderStroke(1.dp, QuickAlarmColors.Outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = QuickAlarmColors.TextPrimary,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.md),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Tertiary action. Text only, muted. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(
            contentColor = QuickAlarmColors.TextSecondary,
        ),
        modifier = modifier,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Staggered entrance. Content starts shifted down and transparent, then eases into
 * place on the shared curve. [index] orders items within a screen.
 */
@Composable
fun Reveal(
    index: Int,
    modifier: Modifier = Modifier,
    delayMs: Int = 120,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(
                durationMillis = Motion.DurationMs,
                delayMillis = delayMs + index * Motion.StaggerMs,
                easing = Motion.Fluid,
            ),
        )
    }
    val offsetPx = with(LocalDensity.current) { 28.dp.toPx() }
    Box(
        modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * offsetPx
        },
    ) { content() }
}

/**
 * Word-by-word reveal for the one large statement per flow. Each word starts muted
 * (about 30 percent opacity) and crosses to full color in reading order.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordRevealText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    baseDelayMs: Int = 350,
) {
    val words = remember(text) { text.split(" ") }
    FlowRow(modifier = modifier) {
        words.forEachIndexed { i, word ->
            val alpha = remember { Animatable(0.3f) }
            LaunchedEffect(Unit) {
                alpha.animateTo(
                    1f,
                    tween(
                        durationMillis = 450,
                        delayMillis = baseDelayMs + i * 70,
                        easing = Motion.Fluid,
                    ),
                )
            }
            Text(
                // NBSP keeps the inter-word gap inside the item; FlowRow wraps between items.
                text = if (i == words.lastIndex) word else "$word ",
                style = style,
                color = QuickAlarmColors.TextPrimary,
                modifier = Modifier.graphicsLayer { this.alpha = alpha.value },
            )
        }
    }
}

/** The one place a gradient is allowed: the hero wordmark, white to gray, on text only. */
@Composable
fun HeroGradientText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            brush = Brush.horizontalGradient(
                listOf(QuickAlarmColors.TextPrimary, QuickAlarmColors.TextSecondary),
            ),
        ),
    )
}

/** "Step 1 of 2" label above setup titles. */
@Composable
fun StepLabel(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Step $current of $total",
        style = MaterialTheme.typography.bodyMedium,
        color = QuickAlarmColors.TextSecondary,
        modifier = modifier,
    )
}

/** Small status indicator with a soft halo. */
@Composable
fun StatusDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(16.dp)) {
        Box(
            Modifier
                .size(16.dp)
                .background(color.copy(alpha = 0.22f), CircleShape),
        )
        Box(
            Modifier
                .size(8.dp)
                .align(androidx.compose.ui.Alignment.Center)
                .background(color, CircleShape),
        )
    }
}

/** Brand mark: a thin-line clock set to seven. Drawn, not an icon font. */
@Composable
fun ClockGlyph(
    modifier: Modifier = Modifier,
    color: Color = QuickAlarmColors.TextPrimary,
) {
    Canvas(modifier) {
        val strokeWidth = 3.dp.toPx()
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - strokeWidth

        drawCircle(color = color, radius = radius, center = center, style = stroke)

        // Minute hand straight up.
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - radius * 0.58f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        // Hour hand at seven.
        val hourAngle = Math.toRadians((7 * 30 - 90).toDouble())
        val hourLength = radius * 0.4f
        drawLine(
            color = color,
            start = center,
            end = Offset(
                center.x + (cos(hourAngle) * hourLength).toFloat(),
                center.y + (sin(hourAngle) * hourLength).toFloat(),
            ),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

/** Success mark: faint ring with a check. Replaces the Material check-circle icon. */
@Composable
fun CheckGlyph(
    modifier: Modifier = Modifier,
    color: Color = QuickAlarmColors.StatusActive,
) {
    Canvas(modifier) {
        val strokeWidth = 3.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth / 2f),
        )
        val w = size.minDimension
        val elbow = Offset(w * 0.44f, w * 0.66f)
        drawLine(
            color = color,
            start = Offset(w * 0.28f, w * 0.52f),
            end = elbow,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = elbow,
            end = Offset(w * 0.72f, w * 0.34f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

/** Error mark: faint ring with an exclamation. Replaces the Material error icon. */
@Composable
fun ErrorGlyph(
    modifier: Modifier = Modifier,
    color: Color = QuickAlarmColors.StatusBlocked,
) {
    Canvas(modifier) {
        val strokeWidth = 3.dp.toPx()
        val radius = size.minDimension / 2f - strokeWidth
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = color.copy(alpha = 0.25f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth / 2f),
        )
        val w = size.minDimension
        drawLine(
            color = color,
            start = Offset(center.x, w * 0.28f),
            end = Offset(center.x, w * 0.56f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = color,
            radius = strokeWidth * 0.7f,
            center = Offset(center.x, w * 0.7f),
        )
    }
}
