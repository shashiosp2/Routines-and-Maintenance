package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class RoutinarySemanticColors(
    val overdueBg: Color,
    val overdueBorder: Color,
    val overdueAccent: Color,
    val dueTodayBg: Color,
    val dueTodayBorder: Color,
    val dueTodayAccent: Color,
    val dueTomorrowBg: Color,
    val dueTomorrowBorder: Color,
    val dueTomorrowAccent: Color,
    val success: Color
)

private val LightSemanticColors = RoutinarySemanticColors(
    overdueBg = PastelRedLight,
    overdueBorder = PastelRedBorderLight,
    overdueAccent = PastelRedTextLight,
    dueTodayBg = PastelOrangeLight,
    dueTodayBorder = PastelOrangeBorderLight,
    dueTodayAccent = PastelOrangeTextLight,
    dueTomorrowBg = PastelYellowLight,
    dueTomorrowBorder = PastelYellowBorderLight,
    dueTomorrowAccent = PastelYellowTextLight,
    success = SuccessGreenLight
)

private val DarkSemanticColors = RoutinarySemanticColors(
    overdueBg = PastelRedDark,
    overdueBorder = PastelRedBorderDark,
    overdueAccent = PastelRedTextDark,
    dueTodayBg = PastelOrangeDark,
    dueTodayBorder = PastelOrangeBorderDark,
    dueTodayAccent = PastelOrangeTextDark,
    dueTomorrowBg = PastelYellowDark,
    dueTomorrowBorder = PastelYellowBorderDark,
    dueTomorrowAccent = PastelYellowTextDark,
    success = SuccessGreenDark
)

val LocalRoutinaryColors = staticCompositionLocalOf { LightSemanticColors }

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = IndigoOnContainerDark,
    secondary = TealSecondaryDark,
    onSecondary = TealOnSecondaryDark,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = TealOnContainerDark,
    tertiary = SlateTertiaryDark,
    tertiaryContainer = SlateContainerDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    errorContainer = ErrorContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = IndigoOnPrimaryLight,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = IndigoOnContainerLight,
    secondary = TealSecondaryLight,
    onSecondary = TealOnSecondaryLight,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = TealOnContainerLight,
    tertiary = SlateTertiaryLight,
    tertiaryContainer = SlateContainerLight,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    errorContainer = ErrorContainerLight
)

@Composable
fun RoutinaryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalRoutinaryColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
