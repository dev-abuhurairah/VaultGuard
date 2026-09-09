package com.vaultguard.app.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Encrypted Database Entity for storing vault items.
 * All sensitive values (passwords, notes, TOTP secrets) are AES-256-GCM encrypted payloads.
 */
@Entity(
    tableName = "vault_items",
    indices = [
        Index(value = ["packageName"]),
        Index(value = ["category"]),
        Index(value = ["isFavorite"])
    ]
)
data class VaultEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val category: String,
    val username: String,
    val encryptedPassword: String,
    val websiteUrl: String,
    val packageName: String,
    val encryptedTotpSecret: String,
    val encryptedNotes: String,
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
