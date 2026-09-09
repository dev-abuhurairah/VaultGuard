package com.vaultguard.app.features.tools

import android.content.Context
import android.net.Uri
import com.vaultguard.app.core.model.VaultCategory
import com.vaultguard.app.core.model.VaultItem
import com.vaultguard.app.core.security.CryptoManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Encrypted Vault Backup & Restore Engine.
 * Supports password-encrypted JSON exports and imports.
 */
object VaultBackupManager {

    /**
     * Serializes items to JSON, encrypts using the backup password, and writes to target URI.
     */
    fun exportEncryptedBackup(
        context: Context,
        uri: Uri,
        items: List<VaultItem>,
        backupPassword: String
    ): Boolean {
        return try {
            val jsonArray = JSONArray()
            for (item in items) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("category", item.category.name)
                    put("username", item.username)
                    put("password", item.password)
                    put("websiteUrl", item.websiteUrl)
                    put("packageName", item.packageName)
                    put("totpSecret", item.totpSecret)
                    put("notes", item.notes)
                    put("isFavorite", item.isFavorite)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                }
                jsonArray.put(obj)
            }

            val plainJson = jsonArray.toString()
            val salt = CryptoManager.generateSalt()
            val derivedKey = CryptoManager.deriveKeyFromPassword(backupPassword.toCharArray(), salt)
            val encryptedPayload = CryptoManager.encrypt(plainJson, derivedKey)

            val exportRoot = JSONObject().apply {
                put("app", "VaultGuard")
                put("version", 1)
                put("salt", java.util.Base64.getEncoder().encodeToString(salt))
                put("data", encryptedPayload)
            }

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                    writer.write(exportRoot.toString(2))
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads and decrypts an encrypted backup file, returning parsed VaultItems.
     */
    fun importEncryptedBackup(
        context: Context,
        uri: Uri,
        backupPassword: String
    ): List<VaultItem>? {
        return try {
            val content = StringBuilder()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        content.append(line)
                    }
                }
            }

            val rootObj = JSONObject(content.toString())
            val saltBase64 = rootObj.getString("salt")
            val encryptedData = rootObj.getString("data")

            val salt = java.util.Base64.getDecoder().decode(saltBase64)
            val derivedKey = CryptoManager.deriveKeyFromPassword(backupPassword.toCharArray(), salt)
            val decryptedJson = CryptoManager.decrypt(encryptedData, derivedKey)

            val jsonArray = JSONArray(decryptedJson)
            val importedItems = mutableListOf<VaultItem>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val item = VaultItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.optString("title", "Untitled"),
                    category = VaultCategory.fromString(obj.optString("category", "LOGIN")),
                    username = obj.optString("username", ""),
                    password = obj.optString("password", ""),
                    websiteUrl = obj.optString("websiteUrl", ""),
                    packageName = obj.optString("packageName", ""),
                    totpSecret = obj.optString("totpSecret", ""),
                    notes = obj.optString("notes", ""),
                    isFavorite = obj.optBoolean("isFavorite", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
                importedItems.add(item)
            }

            importedItems
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
