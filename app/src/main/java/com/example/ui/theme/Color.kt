package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================= ARENA THEME
// v4.0.0 — professional black/graphite redesign requested by the owner.
// Monochrome near-black surfaces, graphite borders, silver-white interactive
// accent. Semantic green/amber/red are kept ONLY for data (markets, status).

val SayvisBlack = Color(0xFF0A0B0E)
val SayvisMidnight = Color(0xFF0F1114)
val SayvisSurface = Color(0xFF14171B)
val SayvisSurfaceVariant = Color(0xFF1C2026)
val SayvisBorder = Color(0xFF272C33)

// Premium actions: deep aurum (kept distinct from the bright accent).
val SayvisGold = Color(0xFFC9A227)
val SayvisGoldLight = Color(0xFFF3CA68)

// Interactive accent: AURUM gold (v5.1.0 atomic-gold identity).
val SayvisCyan = Color(0xFFD4AF37)
val SayvisCyanLight = Color(0xFFF3CA68)

val SayvisSilver = Color(0xFFA6ADB6)
val SayvisMuted = Color(0xFF6E757E)
val SayvisTextLight = Color(0xFFF2F4F7)

// Functional data colors (markets, status) — kept but slightly deepened.
val SayvisGreen = Color(0xFF10B981)
val SayvisAmber = Color(0xFFF5A524)
val SayvisRed = Color(0xFFEF4444)
val SayvisCritical = Color(0xFFDC2626)

// Functional aliases used across SAYVIS interfaces
val SayvisDeepSpace = SayvisBlack
val SayvisGreenSuccess = SayvisGreen
val SayvisAmberWarning = SayvisAmber
val SayvisRedAlert = SayvisRed
val SayvisSilverMuted = SayvisMuted
