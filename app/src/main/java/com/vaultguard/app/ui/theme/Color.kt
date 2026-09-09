package com.vaultguard.app.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Canvas & Surfaces
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFE2E8F0)
val LightBorderSubtle = Color(0xFFE2E8F0)

// Brand Crimson & Scarlet Red Palette
val PrimaryRed = Color(0xFFDC2626)          // Vibrant Crimson Red
val PrimaryRedLight = Color(0xFFEF4444)     // Bright Red Accent
val PrimaryRedDark = Color(0xFFB91C1C)      // Deep Scarlet Red
val PrimaryRedContainer = Color(0xFFFEE2E2) // Soft Red Pill / Accent
val PrimaryRedOnContainer = Color(0xFF991B1B)

// Typography (Crisp contrast on light surfaces)
val TextPrimary = Color(0xFF0F172A)         // Dark slate
val TextSecondary = Color(0xFF475569)       // Medium slate
val TextMuted = Color(0xFF94A3B8)           // Light slate

// Status Accents
val SuccessGreen = Color(0xFF059669)
val WarningOrange = Color(0xFFD97706)
val InfoBlue = Color(0xFF2563EB)
val DangerRed = Color(0xFFDC2626)

// Compatibility Mappings ensuring all screens adapt to Red & Light theme
val DarkBg = LightBg
val DarkSurface = LightSurface
val DarkSurfaceElevated = LightSurfaceElevated
val DarkBorder = LightBorder

val EmeraldAccent = PrimaryRed
val EmeraldLight = PrimaryRedLight
val EmeraldDark = PrimaryRedDark
val PrimaryBlue = PrimaryRed
val PrimaryLight = PrimaryRedLight
