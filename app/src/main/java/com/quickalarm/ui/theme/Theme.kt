package com.quickalarm.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickalarm.R

/**
 * Design tokens for the whole app. One typeface (Manrope), a flat dark palette,
 * spacing from a fixed table, and one shared motion curve. Screens should resolve
 * every visual value through these tokens instead of inventing their own.
 */
object QuickAlarmColors {
    val Background = Color(0xFF000000)
    val Surface = Color(0xFF181818)
    val SurfaceRaised = Color(0xFF1F1F1F)
    val Outline = Color(0xFF313131)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9B9B9B)

    val StatusActive = Color(0xFF6FCF97)
    val StatusHidden = Color(0xFFE2B93B)
    val StatusBlocked = Color(0xFFE57373)
}

/** Spacing table. Values between or outside these are not allowed. */
object Spacing {
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp
}

/** Shared motion: one spring-like curve, one duration, staggered entrances. */
object Motion {
    val Fluid = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    const val DurationMs = 700
    const val ExitMs = 260
    const val StaggerMs = 90
}

/** Manrope variable font; the wght axis is applied per requested weight. */
val ManropeFamily = FontFamily(
    Font(R.font.manrope, FontWeight.Normal),
    Font(R.font.manrope, FontWeight.Medium),
    Font(R.font.manrope, FontWeight.SemiBold),
    Font(R.font.manrope, FontWeight.Bold),
)

private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
)

private val Scheme = darkColorScheme(
    background = QuickAlarmColors.Background,
    onBackground = QuickAlarmColors.TextPrimary,
    surface = QuickAlarmColors.Surface,
    onSurface = QuickAlarmColors.TextPrimary,
    surfaceVariant = QuickAlarmColors.SurfaceRaised,
    onSurfaceVariant = QuickAlarmColors.TextSecondary,
    primary = QuickAlarmColors.TextPrimary,
    onPrimary = QuickAlarmColors.Background,
    outline = QuickAlarmColors.Outline,
    error = QuickAlarmColors.StatusBlocked,
    onError = QuickAlarmColors.Background,
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun QuickAlarmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
