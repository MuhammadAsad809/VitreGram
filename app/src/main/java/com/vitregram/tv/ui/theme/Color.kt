package com.vitregram.tv.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Dark Obsidian Liquid Glass Theme Colors
val ObsidianBackground = Color(0xFF08090D)
val ObsidianSurface = Color(0xFF101218)
val ObsidianCard = Color(0xFF161822)
val ObsidianCardTranslucent = Color(0xCC161822)
val GlassBorder = Color(0x33FFFFFF)
val GlassBorderFocused = Color(0xCCFFFFFF)
val GlassSurfaceHighlight = Color(0x1AFFFFFF)

// Accent Colors (Instagram Signature Gradient)
val InstagramYellow = Color(0xFFFCAF45)
val InstagramOrange = Color(0xFFFF543E)
val InstagramPink = Color(0xFFE1306C)
val InstagramPurple = Color(0xFFC13584)
val InstagramBlue = Color(0xFF405DE6)

val InstagramGradient = Brush.linearGradient(
    colors = listOf(
        InstagramYellow,
        InstagramOrange,
        InstagramPink,
        InstagramPurple,
        InstagramBlue
    )
)

val GlassAccentGradient = Brush.linearGradient(
    colors = listOf(
        Color(0x33E1306C),
        Color(0x33405DE6)
    )
)

// Text Colors
val TextPrimary = Color(0xFFF5F7FA)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)
val ErrorColor = Color(0xFFEF4444)
val SuccessColor = Color(0xFF10B981)
val WarningColor = Color(0xFFF59E0B)
