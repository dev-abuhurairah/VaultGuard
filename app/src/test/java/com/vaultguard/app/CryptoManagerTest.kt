package com.vaultguard.app

import com.vaultguard.app.core.security.CryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator

class CryptoManagerTest {

    @Test
    fun testAesGcmEncryptionDecryptionRoundtrip() {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val secretKey = keyGen.generateKey()

        val sensitivePassword = "SuperSecretPassword#2026!"
        val encrypted = CryptoManager.encrypt(sensitivePassword, secretKey)

        assertNotEquals(sensitivePassword, encrypted)
        assertTrue(encrypted.isNotBlank())

        val decrypted = CryptoManager.decrypt(encrypted, secretKey)
        assertEquals(sensitivePassword, decrypted)
    }

    @Test
    fun testPasswordDerivationAndVerification() {
        val masterPassword = "MySecureMasterKey123$"
        val salt = CryptoManager.generateSalt()
        val derivedKey = CryptoManager.deriveKeyFromPassword(masterPassword.toCharArray(), salt)

        val verifier = CryptoManager.createPasswordVerifier(derivedKey)
        assertTrue(CryptoManager.verifyPassword(verifier, derivedKey))

        // Different password should fail
        val wrongKey = CryptoManager.deriveKeyFromPassword("WrongPassword".toCharArray(), salt)
        assertFalse(CryptoManager.verifyPassword(verifier, wrongKey))
    }

    @Test
    fun testPBKDF2WithCustomIterations() {
        val masterPassword = "CustomIterationPassword789&"
        val salt = CryptoManager.generateSalt()
        val key150k = CryptoManager.deriveKeyFromPassword(masterPassword.toCharArray(), salt, 150_000)
        val key600k = CryptoManager.deriveKeyFromPassword(masterPassword.toCharArray(), salt, 600_000)

        // Different iteration counts produce different cryptographically derived keys
        assertNotEquals(key150k.encoded, key600k.encoded)
        val verifier150k = CryptoManager.createPasswordVerifier(key150k)
        assertTrue(CryptoManager.verifyPassword(verifier150k, key150k))
        assertFalse(CryptoManager.verifyPassword(verifier150k, key600k))
    }
}
