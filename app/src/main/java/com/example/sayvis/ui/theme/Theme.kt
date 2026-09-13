package com.example.sayvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.Typography

private val SayvisDarkColorScheme = darkColorScheme(
    primary = SayvisCyan,
    onPrimary = Color(0xFF030712),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),

    secondary = SayvisGold,
    onSecondary = Color(0xFF1E1B09),
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = Color(0xFFFEF3C7),

    tertiary = SayvisSilver,
    onTertiary = Color(0xFF0F172A),
    tertiaryContainer = Color(0xFF1E293B),
    onTertiaryContainer = Color(0xFFF1F5F9),

    background = SayvisDeepSpace,
    onBackground = SayvisSilver,

    surface = SayvisSurface,
    onSurface = SayvisSilver,
    surfaceVariant = SayvisSurfaceVariant,
    onSurfaceVariant = SayvisSilverMuted,

    outline = SayvisBorder,
    outlineVariant = Color(0xFF1E293B),

    error = SayvisRedAlert,
    onError = Color.White
)

@Composable
fun SayvisTheme(
    accentColor: Color? = null,
    content: @Composable () -> Unit
) {
    // The AI can override the accent color at runtime (self-restyle protocol)
    val scheme = if (accentColor != null) {
        SayvisDarkColorScheme.copy(primary = accentColor)
    } else {
        SayvisDarkColorScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
