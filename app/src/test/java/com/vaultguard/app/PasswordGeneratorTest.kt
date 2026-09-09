package com.vaultguard.app

import com.vaultguard.app.core.model.PasswordStrength
import com.vaultguard.app.features.tools.PasswordGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordGeneratorTest {

    @Test
    fun testPasswordLength() {
        val config16 = PasswordGenerator.GeneratorConfig(length = 16)
        val p16 = PasswordGenerator.generate(config16)
        assertEquals(16, p16.length)

        val config32 = PasswordGenerator.GeneratorConfig(length = 32)
        val p32 = PasswordGenerator.generate(config32)
        assertEquals(32, p32.length)
    }

    @Test
    fun testPassphraseWordCount() {
        val config = PasswordGenerator.GeneratorConfig(
            isPassphrase = true,
            wordCount = 5,
            wordSeparator = "-"
        )
        val phrase = PasswordGenerator.generate(config)
        val words = phrase.split("-")
        assertEquals(5, words.size)
    }

    @Test
    fun testEntropyEvaluation() {
        val weakPass = "12345"
        val strongPass = "k9#Vp!9xL2@qZ&wA"

        assertEquals(PasswordStrength.VERY_WEAK, PasswordGenerator.evaluateStrength(weakPass))
        val strongEvaluation = PasswordGenerator.evaluateStrength(strongPass)
        assertTrue(
            strongEvaluation == PasswordStrength.STRONG || strongEvaluation == PasswordStrength.VERY_STRONG
        )
    }
}
