package com.example.sayvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.Typography

private val SayvisDarkColorScheme = darkColorScheme(
    primary = SayvisCyan,
    onPrimary = Color(0xFF0A0B0E),
    primaryContainer = Color(0xFF3A2F0B),
    onPrimaryContainer = Color(0xFFF3CA68),

    secondary = SayvisGold,
    onSecondary = Color(0xFF0F1114),
    secondaryContainer = Color(0xFF3A2F0B),
    onSecondaryContainer = Color(0xFFF3CA68),

    tertiary = SayvisSilver,
    onTertiary = Color(0xFF0A0B0E),
    tertiaryContainer = Color(0xFF1C2026),
    onTertiaryContainer = Color(0xFFF2F4F7),

    background = SayvisDeepSpace,
    onBackground = SayvisSilver,

    surface = SayvisSurface,
    onSurface = SayvisSilver,
    surfaceVariant = SayvisSurfaceVariant,
    onSurfaceVariant = SayvisSilverMuted,

    outline = SayvisBorder,
    outlineVariant = Color(0xFF20242A),

    error = SayvisRedAlert,
    onError = Color.White
)

@Composable
fun SayvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SayvisDarkColorScheme,
        typography = Typography,
        content = content
    )
}
