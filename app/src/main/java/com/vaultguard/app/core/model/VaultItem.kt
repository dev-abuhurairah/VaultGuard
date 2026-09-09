package com.vaultguard.app.core.model

import java.util.UUID

/**
 * Clean domain model for an unlocked Vault Item.
 */
data class VaultItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val category: VaultCategory = VaultCategory.LOGIN,
    val username: String = "",
    val password: String = "",
    val websiteUrl: String = "",
    val packageName: String = "",
    val totpSecret: String = "",
    val notes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this item matches a given app package name or web domain.
     */
    fun matchesTarget(target: String): Boolean {
        if (target.isBlank()) return false
        val cleanTarget = target.trim().lowercase()
        
        // Match package name e.g. com.google.android.gm
        if (packageName.isNotBlank() && packageName.lowercase() == cleanTarget) {
            return true
        }

        // Match website host/url
        if (websiteUrl.isNotBlank()) {
            val cleanUrl = websiteUrl.lowercase().replace("http://", "").replace("https://", "").replace("www.", "").split("/").first()
            if (cleanUrl.contains(cleanTarget) || cleanTarget.contains(cleanUrl)) {
                return true
            }
        }

        // Match title e.g. "Instagram" against "com.instagram.android"
        if (title.isNotBlank()) {
            val cleanTitle = title.trim().lowercase()
            if (cleanTarget.contains(cleanTitle) || cleanTitle.contains(cleanTarget)) {
                return true
            }
        }

        return false
    }
}
