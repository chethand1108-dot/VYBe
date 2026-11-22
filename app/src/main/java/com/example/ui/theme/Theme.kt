package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VybeDarkColorScheme = darkColorScheme(
    primary = VybeCoral,
    onPrimary = Color.White,
    primaryContainer = VybeCoralDark,
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = VybeViolet,
    onSecondary = Color.White,
    secondaryContainer = VybeVioletDark,
    onSecondaryContainer = Color(0xFFE3DCFF),
    tertiary = VybeAmber,
    onTertiary = Color(0xFF261800),
    tertiaryContainer = VybeAmberDark,
    onTertiaryContainer = Color(0xFFFFE1B3),
    background = VybeObsidian,
    onBackground = VybeInkLight,
    surface = VybeObsidian,
    onSurface = VybeInkLight,
    surfaceVariant = VybeSurfaceElevated,
    onSurfaceVariant = VybeMutedLight,
    surfaceContainerLowest = VybeObsidian,
    surfaceContainerLow = VybeSurface,
    surfaceContainer = VybeSurfaceElevated,
    surfaceContainerHigh = VybeSurfaceHigh,
    outline = VybeOutlineDark,
    outlineVariant = Color(0xFF2A2443),
    error = Color(0xFFFF6B81),
    onError = Color(0xFF3E0010)
)

private val VybeLightColorScheme = lightColorScheme(
    primary = VybeCoralDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCE2),
    onPrimaryContainer = Color(0xFF420014),
    secondary = VybeVioletDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E2FF),
    onSecondaryContainer = Color(0xFF1F0061),
    tertiary = VybeAmberDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8C4),
    onTertiaryContainer = Color(0xFF331D00),
    background = VybeCream,
    onBackground = VybeInkDark,
    surface = VybeLightSurface,
    onSurface = VybeInkDark,
    surfaceVariant = VybeLightCard,
    onSurfaceVariant = VybeMutedDark,
    surfaceContainerLowest = VybeLightSurface,
    surfaceContainerLow = VybeCream,
    surfaceContainer = VybeLightCard,
    surfaceContainerHigh = VybeLightHigh,
    outline = VybeOutlineLight,
    outlineVariant = Color(0xFFEAE5F7),
    error = Color(0xFFBA1A3A),
    onError = Color.White
)

val VybeShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun VybeTheme(
    darkTheme: Boolean = true,
    useSystemTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = if (useSystemTheme) isSystemInDarkTheme() else darkTheme
    val colorScheme = if (isDark) VybeDarkColorScheme else VybeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VybeTypography,
        shapes = VybeShapes,
        content = content
    )
}
