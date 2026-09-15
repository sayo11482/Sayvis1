package com.odin.agent.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val OdinDeepSpace = Color(0xFF0A0E1A)
val OdinSurface = Color(0xFF151A2E)
val OdinSurfaceVariant = Color(0xFF1E2540)
val OdinCyan = Color(0xFF00D4FF)
val OdinGold = Color(0xFFF59E0B)
val OdinGreen = Color(0xFF22C55E)
val OdinRed = Color(0xFFEF4444)
val OdinAmber = Color(0xFFF59E0B)
val OdinSilver = Color(0xFFE0E0FF)
val OdinSilverMuted = Color(0xFF9CA3AF)
val OdinBorder = Color(0xFF2A3450)

private val OdinColorScheme = darkColorScheme(
    primary = OdinCyan,
    secondary = OdinGold,
    background = OdinDeepSpace,
    surface = OdinSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = OdinSilver,
    onSurface = OdinSilver
)

@Composable
fun OdinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OdinColorScheme,
        content = content
    )
}
