package com.vaultguard.app.core.model

/**
 * Result of Watchtower-style security audit across all stored items.
 */
data class SecurityAuditResult(
    val totalAccounts: Int = 0,
    val securityScorePercentage: Int = 100,
    val weakPasswords: List<VaultItem> = emptyList(),
    val reusedPasswords: Map<String, List<VaultItem>> = emptyMap(),
    val missing2FaLogins: List<VaultItem> = emptyList(),
    val oldPasswords: List<VaultItem> = emptyList()
)
