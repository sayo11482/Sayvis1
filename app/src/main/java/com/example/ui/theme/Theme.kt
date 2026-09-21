package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SayvisColorScheme = darkColorScheme(
    primary = SayvisCyan,
    onPrimary = SayvisBlack,
    primaryContainer = SayvisSurfaceVariant,
    onPrimaryContainer = SayvisCyanLight,
    secondary = SayvisGold,
    onSecondary = SayvisBlack,
    secondaryContainer = Color(0xFF2E2612),
    onSecondaryContainer = SayvisGoldLight,
    tertiary = SayvisSilver,
    onTertiary = SayvisBlack,
    background = SayvisBlack,
    onBackground = SayvisTextLight,
    surface = SayvisMidnight,
    onSurface = SayvisTextLight,
    surfaceVariant = SayvisSurface,
    onSurfaceVariant = SayvisSilver,
    outline = SayvisBorder,
    error = SayvisRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SayvisColorScheme,
        typography = Typography,
        content = content
    )
}

