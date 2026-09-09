package com.vaultguard.app.features.tools

import com.vaultguard.app.core.model.PasswordStrength
import com.vaultguard.app.core.model.SecurityAuditResult
import com.vaultguard.app.core.model.VaultCategory
import com.vaultguard.app.core.model.VaultItem

/**
 * Watchtower-style Security Auditor.
 * Inspects all stored credentials to detect weak, reused, or compromised passwords.
 */
object SecurityAuditManager {

    private val NINETY_DAYS_MILLIS = 90L * 24 * 60 * 60 * 1000L

    fun runAudit(items: List<VaultItem>): SecurityAuditResult {
        val logins = items.filter { it.category == VaultCategory.LOGIN && it.password.isNotBlank() }
        if (logins.isEmpty()) {
            return SecurityAuditResult()
        }

        // 1. Weak passwords
        val weakList = logins.filter {
            val strength = PasswordGenerator.evaluateStrength(it.password)
            strength == PasswordStrength.VERY_WEAK || strength == PasswordStrength.WEAK
        }

        // 2. Reused passwords
        val groupedByPassword = logins.groupBy { it.password }
        val reusedMap = groupedByPassword.filter { it.value.size > 1 }

        // 3. Missing 2FA
        val missing2Fa = logins.filter { it.totpSecret.isBlank() }

        // 4. Old passwords (> 90 days since last update)
        val now = System.currentTimeMillis()
        val oldList = logins.filter { (now - it.updatedAt) > NINETY_DAYS_MILLIS }

        // Calculate overall score (0 - 100%)
        var deductions = 0
        deductions += (weakList.size * 15)
        deductions += (reusedMap.values.flatten().size * 10)
        deductions += (oldList.size * 5)
        val score = (100 - deductions).coerceIn(0, 100)

        return SecurityAuditResult(
            totalAccounts = logins.size,
            securityScorePercentage = score,
            weakPasswords = weakList,
            reusedPasswords = reusedMap,
            missing2FaLogins = missing2Fa,
            oldPasswords = oldList
        )
    }
}
