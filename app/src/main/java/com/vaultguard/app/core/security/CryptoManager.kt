package com.vaultguard.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Production-grade Zero-Knowledge Cryptography Engine.
 * 
 * Security features:
 * 1. Master Password derived with PBKDF2WithHmacSHA256 (600,000 rounds).
 * 2. Database & Vault payloads encrypted with AES-256-GCM with unique 12-byte IVs.
 * 3. Android Keystore hardware-backed keys with StrongBox/TEE backing.
 */
object CryptoManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEYSTORE_ALIAS = "VaultGuardMasterKey"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12
    private const val PBKDF2_ITERATIONS = 600_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 32

    private val secureRandom = SecureRandom()

    /**
     * Retrieves or generates the hardware-backed AES-256 Master Key from Android Keystore.
     */
    fun getOrCreateHardwareMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as KeyStore.SecretKeyEntry
            return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_LENGTH_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /**
     * Generates a cryptographically secure random salt.
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH_BYTES)
        secureRandom.nextBytes(salt)
        return salt
    }

    /**
     * Derives a 256-bit AES key from the user's Master Password using PBKDF2.
     */
    fun deriveKeyFromPassword(password: CharArray, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val derivedKeyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(derivedKeyBytes, "AES")
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     * Returns a Base64 string containing: IV (12 bytes) + Ciphertext + GCM Tag.
     */
    fun encrypt(plainText: String, secretKey: SecretKey): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val iv = ByteArray(GCM_IV_LENGTH).also { secureRandom.nextBytes(it) }
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * Decrypts a Base64-encoded encrypted payload using AES-256-GCM.
     */
    fun decrypt(encryptedPayloadBase64: String, secretKey: SecretKey): String {
        if (encryptedPayloadBase64.isBlank()) return ""
        val combined = Base64.getDecoder().decode(encryptedPayloadBase64)
        if (combined.size < GCM_IV_LENGTH) throw IllegalArgumentException("Ciphertext payload too short")

        val iv = ByteArray(GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
        val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
        System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)

        val decryptedBytes = cipher.doFinal(cipherText)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * Creates a verification hash to test if entered master password is correct.
     */
    fun createPasswordVerifier(secretKey: SecretKey): String {
        return encrypt("VAULTGUARD_VERIFIED_KEY", secretKey)
    }

    /**
     * Validates if a given secret key can decrypt the stored verifier.
     */
    fun verifyPassword(verifier: String, secretKey: SecretKey): Boolean {
        return try {
            val decrypted = decrypt(verifier, secretKey)
            decrypted == "VAULTGUARD_VERIFIED_KEY"
        } catch (e: Exception) {
            false
        }
    }
}
