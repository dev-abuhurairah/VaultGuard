package com.vaultguard.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultguard.app.core.repository.VaultRepository
import com.vaultguard.app.core.security.BiometricAuthManager
import com.vaultguard.app.features.tools.VaultBackupManager
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    repository: VaultRepository,
    onManagePermissions: () -> Unit,
    onLockVault: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var biometricEnabled by remember { mutableStateOf(repository.isBiometricEnabled()) }
    var screenProtectionEnabled by remember { mutableStateOf(repository.isScreenProtectionEnabled()) }
    var autoLockMinutes by remember { mutableStateOf(repository.getAutoLockMinutes()) }
    var autoClearClipboardSec by remember { mutableStateOf(repository.getAutoClearClipboardSeconds()) }

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }

    val biometricManager = remember { BiometricAuthManager(context) }
    val isBiometricAvailable = remember {
        biometricManager.canAuthenticate() == BiometricAuthManager.BiometricStatus.AVAILABLE
    }

    // Backup Export File Picker
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && backupPassword.isNotBlank()) {
            scope.launch {
                val items = repository.getAllItems()
                val success = VaultBackupManager.exportEncryptedBackup(context, uri, items, backupPassword)
                if (success) {
                    Toast.makeText(context, "Encrypted backup exported successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
                backupPassword = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Security policies, auto-fill, and vault configuration",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Security Section
        Text(text = "SECURITY & PRIVACY", color = Color(0xFF059669), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingsSwitchRow(
                    title = "Biometric Unlock",
                    subtitle = if (isBiometricAvailable) "Unlock with fingerprint or face" else "No biometric hardware detected",
                    icon = Icons.Default.Fingerprint,
                    checked = biometricEnabled && isBiometricAvailable,
                    enabled = isBiometricAvailable,
                    onCheckedChange = {
                        biometricEnabled = it
                        repository.enableBiometric(it)
                    }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = "Screen Protection (FLAG_SECURE)",
                    subtitle = "Block screenshots and hide app in recent tasks",
                    icon = Icons.Default.VisibilityOff,
                    checked = screenProtectionEnabled,
                    onCheckedChange = {
                        screenProtectionEnabled = it
                        repository.setScreenProtectionEnabled(it)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Timeouts Section
        Text(text = "TIMERS & AUTOMATION", color = Color(0xFF059669), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingsClickableRow(
                    title = "Auto-Lock Timeout",
                    subtitle = if (autoLockMinutes == 0) "Immediately on background" else "After $autoLockMinutes minutes of inactivity",
                    icon = Icons.Default.Schedule,
                    onClick = {
                        val next = when (autoLockMinutes) {
                            1 -> 5
                            5 -> 15
                            15 -> 0
                            else -> 1
                        }
                        autoLockMinutes = next
                        repository.setAutoLockMinutes(next)
                    }
                )

                SettingsDivider()

                SettingsClickableRow(
                    title = "Clear Clipboard Timer",
                    subtitle = "Automatically wipe copied passwords in $autoClearClipboardSec seconds",
                    icon = Icons.Default.Timer,
                    onClick = {
                        val next = when (autoClearClipboardSec) {
                            15 -> 30
                            30 -> 60
                            else -> 15
                        }
                        autoClearClipboardSec = next
                        repository.setAutoClearClipboardSeconds(next)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System & Permissions
        Text(text = "SYSTEM & DETECTION", color = Color(0xFF059669), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingsClickableRow(
                    title = "Manage Permissions",
                    subtitle = "Configure Autofill, Accessibility, and Overlays",
                    icon = Icons.Default.Security,
                    onClick = onManagePermissions
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Vault Backup & Restore
        Text(text = "BACKUP & RESTORE", color = Color(0xFF059669), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingsClickableRow(
                    title = "Export Encrypted Backup",
                    subtitle = "Password-protected AES-256 JSON file",
                    icon = Icons.Default.Upload,
                    onClick = { showBackupDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lock Vault Button
        Button(
            onClick = onLockVault,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lock Vault Now", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Export Password Dialog
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            containerColor = Color(0xFF131B2E),
            title = { Text("Set Backup Password", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Your exported file will be encrypted with this password using AES-256-GCM.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Backup Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backupPassword.length < 8) {
                            Toast.makeText(context, "Password must be at least 8 chars", Toast.LENGTH_SHORT).show()
                        } else {
                            showBackupDialog = false
                            exportLauncher.launch("vaultguard_backup_${System.currentTimeMillis()}.json")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF059669),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}

@Composable
fun SettingsClickableRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF64748B))
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF1E293B))
    )
}
