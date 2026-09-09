package com.vaultguard.app.core.repository

import android.content.Context
import android.content.SharedPreferences
import java.util.Base64
import com.vaultguard.app.core.database.VaultDao
import com.vaultguard.app.core.database.VaultDatabase
import com.vaultguard.app.core.database.VaultEntity
import com.vaultguard.app.core.model.VaultCategory
import com.vaultguard.app.core.model.VaultItem
import com.vaultguard.app.core.security.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.crypto.SecretKey

/**
 * VaultRepository provides unified, secure access to the Vault.
 * Manages zero-knowledge encryption/decryption, unlocked session state, and local storage.
 */
class VaultRepository private constructor(
    private val context: Context,
    private val vaultDao: VaultDao
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vaultguard_security_prefs", Context.MODE_PRIVATE)

    // Unlocked in-memory Master Key. Cleared to null when locked.
    private var activeMasterKey: SecretKey? = null

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var lastActiveTime: Long = System.currentTimeMillis()

    companion object {
        private const val PREF_SALT = "pref_salt"
        private const val PREF_PASSWORD_VERIFIER = "pref_password_verifier"
        private const val PREF_BIOMETRIC_ENABLED = "pref_biometric_enabled"
        private const val PREF_SCREEN_PROTECTION = "pref_screen_protection"
        private const val PREF_AUTO_LOCK_MINUTES = "pref_auto_lock_minutes"
        private const val PREF_AUTO_CLEAR_CLIPBOARD_SEC = "pref_auto_clear_clipboard_sec"

        @Volatile
        private var INSTANCE: VaultRepository? = null

        fun getInstance(context: Context): VaultRepository {
            return INSTANCE ?: synchronized(this) {
                val db = VaultDatabase.getInstance(context)
                val instance = VaultRepository(context.applicationContext, db.vaultDao())
                INSTANCE = instance
                instance
            }
        }
    }

    private const val PREF_PBKDF2_ITERATIONS = "pref_pbkdf2_iterations"

    fun isVaultSetup(): Boolean {
        return prefs.contains(PREF_PASSWORD_VERIFIER) && prefs.contains(PREF_SALT)
    }

    /**
     * Initializes the Vault with a new Master Password on Dispatchers.Default.
     * Automatically binds the master key to the hardware Keystore for seamless biometrics.
     */
    suspend fun setupMasterPassword(password: String): Boolean = withContext(Dispatchers.Default) {
        try {
            val salt = CryptoManager.generateSalt()
            val iterations = CryptoManager.PBKDF2_ITERATIONS
            val derivedKey = CryptoManager.deriveKeyFromPassword(password.toCharArray(), salt, iterations)
            val verifier = CryptoManager.createPasswordVerifier(derivedKey)

            prefs.edit()
                .putString(PREF_SALT, Base64.getEncoder().encodeToString(salt))
                .putString(PREF_PASSWORD_VERIFIER, verifier)
                .putInt(PREF_PBKDF2_ITERATIONS, iterations)
                .apply()

            activeMasterKey = derivedKey
            _isUnlocked.value = true
            lastActiveTime = System.currentTimeMillis()

            // Automatically bind master key to hardware Keystore for biometrics
            syncBiometricKey(derivedKey)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Verifies the entered Master Password and unlocks the session on Dispatchers.Default.
     * Automatically synchronizes the Keystore biometric key on successful unlock.
     */
    suspend fun unlockWithPassword(password: String): Boolean = withContext(Dispatchers.Default) {
        val saltBase64 = prefs.getString(PREF_SALT, null) ?: return@withContext false
        val verifier = prefs.getString(PREF_PASSWORD_VERIFIER, null) ?: return@withContext false
        val iterations = prefs.getInt(PREF_PBKDF2_ITERATIONS, CryptoManager.PBKDF2_ITERATIONS)

        val salt = try {
            Base64.getDecoder().decode(saltBase64)
        } catch (e: Exception) {
            return@withContext false
        }

        // 1. Try with configured/standard iterations
        val derivedKey = CryptoManager.deriveKeyFromPassword(password.toCharArray(), salt, iterations)
        if (CryptoManager.verifyPassword(verifier, derivedKey)) {
            activeMasterKey = derivedKey
            _isUnlocked.value = true
            lastActiveTime = System.currentTimeMillis()
            syncBiometricKey(derivedKey)
            return@withContext true
        }

        // 2. Fallback check: if previously created with 600,000 iterations, migrate seamlessly to 150,000
        if (iterations != 600_000) {
            try {
                val legacyKey = CryptoManager.deriveKeyFromPassword(password.toCharArray(), salt, 600_000)
                if (CryptoManager.verifyPassword(verifier, legacyKey)) {
                    val fastKey = CryptoManager.deriveKeyFromPassword(password.toCharArray(), salt, CryptoManager.PBKDF2_ITERATIONS)
                    val fastVerifier = CryptoManager.createPasswordVerifier(fastKey)

                    prefs.edit()
                        .putString(PREF_PASSWORD_VERIFIER, fastVerifier)
                        .putInt(PREF_PBKDF2_ITERATIONS, CryptoManager.PBKDF2_ITERATIONS)
                        .apply()

                    activeMasterKey = fastKey
                    _isUnlocked.value = true
                    lastActiveTime = System.currentTimeMillis()
                    syncBiometricKey(fastKey)
                    return@withContext true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return@withContext false
    }

    /**
     * Unlocks the vault via biometric authentication by decrypting the hardware-bound master key.
     */
    fun unlockWithBiometric(): Boolean {
        val verifier = prefs.getString(PREF_PASSWORD_VERIFIER, null) ?: return false
        val encryptedMasterKey = prefs.getString("pref_enc_master_key", null) ?: return false

        return try {
            val hwKey = CryptoManager.getOrCreateHardwareMasterKey()
            val decryptedKeyBytes = Base64.getDecoder().decode(CryptoManager.decrypt(encryptedMasterKey, hwKey))
            val secretKey = javax.crypto.spec.SecretKeySpec(decryptedKeyBytes, "AES")

            if (CryptoManager.verifyPassword(verifier, secretKey)) {
                activeMasterKey = secretKey
                _isUnlocked.value = true
                lastActiveTime = System.currentTimeMillis()
                prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, true).apply()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Binds the master key to the hardware-backed Keystore for biometric unlocking.
     */
    fun syncBiometricKey(key: SecretKey = activeMasterKey ?: return false): Boolean {
        return try {
            val hwKey = CryptoManager.getOrCreateHardwareMasterKey()
            val masterKeyBase64 = Base64.getEncoder().encodeToString(key.encoded)
            val encMasterKey = CryptoManager.encrypt(masterKeyBase64, hwKey)
            prefs.edit()
                .putString("pref_enc_master_key", encMasterKey)
                .putBoolean(PREF_BIOMETRIC_ENABLED, true)
                .apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun isBiometricKeySynced(): Boolean {
        return prefs.contains("pref_enc_master_key")
    }

    fun enableBiometric(enable: Boolean) {
        prefs.edit().putBoolean(PREF_BIOMETRIC_ENABLED, enable).apply()
        if (enable && activeMasterKey != null) {
            syncBiometricKey(activeMasterKey!!)
        } else if (!enable) {
            prefs.edit().remove("pref_enc_master_key").apply()
        }
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(PREF_BIOMETRIC_ENABLED, false) && isBiometricKeySynced()

    fun isScreenProtectionEnabled(): Boolean = prefs.getBoolean(PREF_SCREEN_PROTECTION, true)
    fun setScreenProtectionEnabled(enabled: Boolean) = prefs.edit().putBoolean(PREF_SCREEN_PROTECTION, enabled).apply()

    fun getAutoLockMinutes(): Int = prefs.getInt(PREF_AUTO_LOCK_MINUTES, 5)
    fun setAutoLockMinutes(minutes: Int) = prefs.edit().putInt(PREF_AUTO_LOCK_MINUTES, minutes).apply()

    fun getAutoClearClipboardSeconds(): Int = prefs.getInt(PREF_AUTO_CLEAR_CLIPBOARD_SEC, 30)
    fun setAutoClearClipboardSeconds(seconds: Int) = prefs.edit().putInt(PREF_AUTO_CLEAR_CLIPBOARD_SEC, seconds).apply()

    /**
     * Locks the vault and wipes the master key from memory.
     */
    fun lock() {
        activeMasterKey = null
        _isUnlocked.value = false
    }

    fun touch() {
        lastActiveTime = System.currentTimeMillis()
    }

    fun checkAutoLock() {
        val timeoutMinutes = getAutoLockMinutes()
        if (timeoutMinutes <= 0) return
        val elapsed = System.currentTimeMillis() - lastActiveTime
        if (elapsed > timeoutMinutes * 60 * 1000L) {
            lock()
        }
    }

    // --- Vault CRUD Operations ---

    fun getAllItemsFlow(): Flow<List<VaultItem>> {
        return vaultDao.getAllItemsFlow().map { entities ->
            entities.map { decryptEntity(it) }
        }
    }

    suspend fun getAllItems(): List<VaultItem> {
        return vaultDao.getAllItems().map { decryptEntity(it) }
    }

    suspend fun getItemById(id: String): VaultItem? {
        val entity = vaultDao.getItemById(id) ?: return null
        return decryptEntity(entity)
    }

    suspend fun findMatchingItems(target: String): List<VaultItem> {
        val entities = vaultDao.findMatchingItems(target)
        val decrypted = entities.map { decryptEntity(it) }
        return decrypted.filter { it.matchesTarget(target) }
    }

    suspend fun saveItem(item: VaultItem) {
        val entity = encryptItem(item)
        vaultDao.insert(entity)
        touch()
    }

    suspend fun deleteItem(item: VaultItem) {
        vaultDao.deleteById(item.id)
        touch()
    }

    suspend fun clearAll() {
        vaultDao.clearAll()
    }

    private fun encryptItem(item: VaultItem): VaultEntity {
        val key = activeMasterKey
            ?: throw IllegalStateException("Cannot encrypt vault item: Vault is locked!")

        val encPassword = CryptoManager.encrypt(item.password, key)
        val encTotp = if (item.totpSecret.isNotBlank()) CryptoManager.encrypt(item.totpSecret, key) else ""
        val encNotes = if (item.notes.isNotBlank()) CryptoManager.encrypt(item.notes, key) else ""

        return VaultEntity(
            id = item.id,
            title = item.title,
            category = item.category.name,
            username = item.username,
            encryptedPassword = encPassword,
            websiteUrl = item.websiteUrl,
            packageName = item.packageName,
            encryptedTotpSecret = encTotp,
            encryptedNotes = encNotes,
            isFavorite = item.isFavorite,
            createdAt = item.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun decryptEntity(entity: VaultEntity): VaultItem {
        val key = activeMasterKey
        val password = if (key != null && entity.encryptedPassword.isNotBlank()) {
            try { CryptoManager.decrypt(entity.encryptedPassword, key) } catch (e: Exception) { "••••••••" }
        } else {
            "••••••••"
        }

        val totp = if (key != null && entity.encryptedTotpSecret.isNotBlank()) {
            try { CryptoManager.decrypt(entity.encryptedTotpSecret, key) } catch (e: Exception) { "" }
        } else {
            ""
        }

        val notes = if (key != null && entity.encryptedNotes.isNotBlank()) {
            try { CryptoManager.decrypt(entity.encryptedNotes, key) } catch (e: Exception) { "" }
        } else {
            ""
        }

        return VaultItem(
            id = entity.id,
            title = entity.title,
            category = VaultCategory.fromString(entity.category),
            username = entity.username,
            password = password,
            websiteUrl = entity.websiteUrl,
            packageName = entity.packageName,
            totpSecret = totp,
            notes = notes,
            isFavorite = entity.isFavorite,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
