package com.vaultguard.app.features.tools

import com.vaultguard.app.core.model.PasswordStrength
import java.security.SecureRandom
import kotlin.math.log2

/**
 * Advanced Password & Passphrase Generator with Shannon Entropy analysis.
 */
object PasswordGenerator {

    private val secureRandom = SecureRandom()

    private const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    private const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
    private const val SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?"
    private const val AMBIGUOUS = "0Oo1lI|`'\"~"

    private val WORD_LIST = listOf(
        "anchor", "beacon", "castle", "dolphin", "emerald", "falcon", "galaxy", "harbor",
        "island", "jaguar", "knight", "legacy", "meteor", "nebula", "oasis", "phoenix",
        "quantum", "radar", "shadow", "timber", "ultra", "vortex", "whisper", "zenith",
        "arctic", "breeze", "crystal", "dragon", "echo", "frost", "glacier", "horizon",
        "inferno", "jungle", "kinetic", "lunar", "monarch", "nova", "orbit", "plasma"
    )

    data class GeneratorConfig(
        val length: Int = 16,
        val includeUppercase: Boolean = true,
        val includeLowercase: Boolean = true,
        val includeDigits: Boolean = true,
        val includeSymbols: Boolean = true,
        val excludeAmbiguous: Boolean = true,
        val isPassphrase: Boolean = false,
        val wordCount: Int = 4,
        val wordSeparator: String = "-"
    )

    /**
     * Generates a secure random password or passphrase based on config.
     */
    fun generate(config: GeneratorConfig = GeneratorConfig()): String {
        if (config.isPassphrase) {
            val words = (1..config.wordCount).map {
                WORD_LIST[secureRandom.nextInt(WORD_LIST.size)]
            }
            return words.joinToString(config.wordSeparator)
        }

        var charPool = StringBuilder()
        val guaranteedChars = mutableListOf<Char>()

        if (config.includeLowercase) {
            val set = if (config.excludeAmbiguous) LOWERCASE.filter { it !in AMBIGUOUS } else LOWERCASE
            charPool.append(set)
            guaranteedChars.add(set[secureRandom.nextInt(set.length)])
        }
        if (config.includeUppercase) {
            val set = if (config.excludeAmbiguous) UPPERCASE.filter { it !in AMBIGUOUS } else UPPERCASE
            charPool.append(set)
            guaranteedChars.add(set[secureRandom.nextInt(set.length)])
        }
        if (config.includeDigits) {
            val set = if (config.excludeAmbiguous) DIGITS.filter { it !in AMBIGUOUS } else DIGITS
            charPool.append(set)
            guaranteedChars.add(set[secureRandom.nextInt(set.length)])
        }
        if (config.includeSymbols) {
            val set = if (config.excludeAmbiguous) SYMBOLS.filter { it !in AMBIGUOUS } else SYMBOLS
            charPool.append(set)
            guaranteedChars.add(set[secureRandom.nextInt(set.length)])
        }

        if (charPool.isEmpty()) {
            charPool.append(LOWERCASE)
            guaranteedChars.add(LOWERCASE[secureRandom.nextInt(LOWERCASE.length)])
        }

        val poolStr = charPool.toString()
        val remainingLength = (config.length - guaranteedChars.size).coerceAtLeast(0)
        val passwordChars = ArrayList(guaranteedChars)

        for (i in 0 until remainingLength) {
            passwordChars.add(poolStr[secureRandom.nextInt(poolStr.length)])
        }

        // Shuffle guaranteed characters to avoid predictable placement
        passwordChars.shuffle(secureRandom)
        return passwordChars.joinToString("")
    }

    /**
     * Calculates Shannon entropy in bits for a given password.
     */
    fun calculateEntropy(password: String): Double {
        if (password.isEmpty()) return 0.0

        var poolSize = 0
        if (password.any { it.isLowerCase() }) poolSize += 26
        if (password.any { it.isUpperCase() }) poolSize += 26
        if (password.any { it.isDigit() }) poolSize += 10
        if (password.any { !it.isLetterOrDigit() }) poolSize += 32

        if (poolSize == 0) poolSize = 1
        return password.length * log2(poolSize.toDouble())
    }

    /**
     * Evaluates password strength based on entropy and length.
     */
    fun evaluateStrength(password: String): PasswordStrength {
        if (password.length < 6) return PasswordStrength.VERY_WEAK
        val entropy = calculateEntropy(password)

        return when {
            entropy < 36 -> PasswordStrength.VERY_WEAK
            entropy < 50 -> PasswordStrength.WEAK
            entropy < 70 -> PasswordStrength.FAIR
            entropy < 90 -> PasswordStrength.STRONG
            else -> PasswordStrength.VERY_STRONG
        }
    }
}
