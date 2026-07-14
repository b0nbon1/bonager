package com.bonvic.bonager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object BonagerColors {
    val Background = Color(0xFFF5F3ED)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceAlt = Color(0xFFEDF3F1)
    val SurfaceElevated = Color(0xFFFAF9F5)
    val Ink = Color(0xFF111918)
    val InkSecondary = Color(0xFF2C3534)
    val Muted = Color(0xFF637370)
    val Border = Color(0xFFD8DDD9)
    val BorderLight = Color(0xFFEBEEEB)
    val Primary = Color(0xFF0A7A70)
    val PrimaryDark = Color(0xFF065C54)
    val PrimaryLight = Color(0xFFE1F3F0)
    val Accent = Color(0xFFD4940F)
    val AccentLight = Color(0xFFFDF3DC)
    val Danger = Color(0xFFBF4040)
    val DangerLight = Color(0xFFFAECEC)
    val Blue = Color(0xFF2F62A0)
    val BlueLight = Color(0xFFE8EFF8)
    val Plum = Color(0xFF725480)
    val PlumLight = Color(0xFFF2ECF6)
    val Success = Color(0xFF287348)
    val SuccessLight = Color(0xFFE5F4EC)
    val Tab = Color(0xFFFCFBF7)
}

private val bonagerTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Black, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 52.sp),
    displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

private val colors = lightColorScheme(
    primary = BonagerColors.Primary,
    onPrimary = BonagerColors.Surface,
    primaryContainer = BonagerColors.PrimaryLight,
    onPrimaryContainer = BonagerColors.PrimaryDark,
    secondary = BonagerColors.Blue,
    onSecondary = BonagerColors.Surface,
    secondaryContainer = BonagerColors.BlueLight,
    onSecondaryContainer = BonagerColors.Blue,
    tertiary = BonagerColors.Plum,
    onTertiary = BonagerColors.Surface,
    tertiaryContainer = BonagerColors.PlumLight,
    background = BonagerColors.Background,
    onBackground = BonagerColors.Ink,
    surface = BonagerColors.Surface,
    onSurface = BonagerColors.Ink,
    surfaceVariant = BonagerColors.SurfaceAlt,
    onSurfaceVariant = BonagerColors.Muted,
    outline = BonagerColors.Border,
    outlineVariant = BonagerColors.BorderLight,
    error = BonagerColors.Danger,
    errorContainer = BonagerColors.DangerLight,
)

@Composable
fun BonagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        typography = bonagerTypography,
        content = content,
    )
}
