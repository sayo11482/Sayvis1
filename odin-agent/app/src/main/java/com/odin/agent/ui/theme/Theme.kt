package com.odin.agent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ODIN Final Pure Black Professional Theme - No Sayvis
// Pure black #000000 background, gold accents, professional trading

val OdinDeepSpace = Color(0xFF000000) // Pure black - professional
val OdinSurface = Color(0xFF0A0A0A) // Very dark gray for cards
val OdinSurfaceVariant = Color(0xFF141414) // Slightly lighter for elevated
val OdinSurfaceGold = Color(0xFF1A1505) // Gold tinted black

// Gold palette - rich gold from pitbull chain
val OdinGold = Color(0xFFD4AF37) // Main gold
val OdinGoldLight = Color(0xFFFFD700) // Bright gold
val OdinGoldDark = Color(0xFFB8941F) // Dark gold
val OdinGoldMetallic = Color(0xFFC5A028)

// Tech accents
val OdinCyan = Color(0xFF00D4FF)
val OdinCyanMuted = Color(0xFF0EA5E9)

// Trading
val OdinGreen = Color(0xFF00FF88) // Neon green for buy
val OdinGreenBright = Color(0xFF10B981)
val OdinRed = Color(0xFFFF3344) // Neon red for sell
val OdinRedBright = Color(0xFFDC2626)
val OdinAmber = Color(0xFFF59E0B)

// Text - pure white on black for max contrast
val OdinSilver = Color(0xFFE5E5E5)
val OdinSilverMuted = Color(0xFF888888)
val OdinSilverDim = Color(0xFF555555)
val OdinWhite = Color(0xFFFFFFFF)

// Borders - subtle on black
val OdinBorder = Color(0xFF1A1A1A)
val OdinBorderGold = Color(0xFFD4AF37).copy(alpha = 0.4f)
val OdinBorderCyan = Color(0xFF00D4FF).copy(alpha = 0.2f)

private val OdinColorScheme = darkColorScheme(
    primary = OdinGold,
    secondary = OdinCyan,
    tertiary = OdinGoldLight,
    background = OdinDeepSpace,
    surface = OdinSurface,
    surfaceVariant = OdinSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = OdinWhite,
    onSurface = OdinSilver,
    outline = OdinBorder
)

@Composable
fun OdinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OdinColorScheme,
        typography = OdinTypography,
        content = content
    )
}
