package com.odin.agent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ODIN Final Integrated Theme - Based on Pitbull Gold Image
// Dark professional trading theme with gold accents

val OdinDeepSpace = Color(0xFF080B12) // Deeper dark like image background
val OdinSurface = Color(0xFF111827) // Card background
val OdinSurfaceVariant = Color(0xFF1A2236) // Elevated card
val OdinSurfaceGold = Color(0xFF1E2A1A) // Gold tinted surface

// Gold palette from image - rich gold chain
val OdinGold = Color(0xFFD4AF37) // Main gold from chain
val OdinGoldLight = Color(0xFFFFD700) // Bright gold
val OdinGoldDark = Color(0xFFB8941F) // Dark gold
val OdinGoldMetallic = Color(0xFFC5A028) // Metallic gold

// Cyan/Blue for tech accents (robotic hand, charts)
val OdinCyan = Color(0xFF00D4FF)
val OdinCyanMuted = Color(0xFF0EA5E9)

// Trading colors
val OdinGreen = Color(0xFF22C55E)
val OdinGreenBright = Color(0xFF10B981)
val OdinRed = Color(0xFFEF4444)
val OdinRedBright = Color(0xFFDC2626)
val OdinAmber = Color(0xFFF59E0B)

// Text colors
val OdinSilver = Color(0xFFE5E7EB)
val OdinSilverMuted = Color(0xFF9CA3AF)
val OdinSilverDim = Color(0xFF6B7280)
val OdinWhite = Color(0xFFFFFFFF)

// Borders and dividers
val OdinBorder = Color(0xFF1F2937)
val OdinBorderGold = Color(0xFFD4AF37).copy(alpha = 0.3f)
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
    onBackground = OdinSilver,
    onSurface = OdinSilver,
    outline = OdinBorder
)

@Composable
fun OdinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OdinColorScheme,
        content = content
    )
}
