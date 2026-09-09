package com.vaultguard.app.core.model

import androidx.compose.ui.graphics.Color

/**
 * Evaluated password strength rating and score.
 */
enum class PasswordStrength(
    val label: String,
    val score: Int, // 0 to 4
    val color: Long
) {
    VERY_WEAK("Very Weak", 0, 0xFFEF4444),
    WEAK("Weak", 1, 0xFFF97316),
    FAIR("Fair", 2, 0xFFFACC15),
    STRONG("Strong", 3, 0xFF10B981),
    VERY_STRONG("Military Grade", 4, 0xFF059669);

    fun toComposeColor(): Color = Color(color)
}
