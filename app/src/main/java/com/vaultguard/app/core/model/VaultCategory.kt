package com.vaultguard.app.core.model

/**
 * Supported categories for Vault items.
 * Inspired by 1Password and Bitwarden item types.
 */
enum class VaultCategory(val displayName: String) {
    LOGIN("Logins"),
    SECURE_NOTE("Secure Notes"),
    CREDIT_CARD("Payment Cards"),
    TOTP_2FA("2FA Authenticator"),
    IDENTITY("Identities"),
    WIFI("Wi-Fi Passwords");

    companion object {
        fun fromString(value: String): VaultCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: LOGIN
        }
    }
}
