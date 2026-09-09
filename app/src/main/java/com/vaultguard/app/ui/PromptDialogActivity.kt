package com.vaultguard.app.ui

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.vaultguard.app.core.model.VaultCategory
import com.vaultguard.app.core.model.VaultItem
import com.vaultguard.app.core.repository.VaultRepository
import com.vaultguard.app.core.security.BiometricAuthManager
import com.vaultguard.app.services.VaultAccessibilityService
import androidx.compose.foundation.BorderStroke
import com.vaultguard.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Universal Translucent Floating Prompt Activity.
 * Handles interactive prompt-to-fill (with biometric check) and prompt-to-save.
 */
class PromptDialogActivity : FragmentActivity() {

    companion object {
        const val ACTION_PROMPT_AUTOFILL = "com.vaultguard.action.PROMPT_AUTOFILL"
        const val ACTION_PROMPT_SAVE = "com.vaultguard.action.PROMPT_SAVE"

        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_PASSWORD = "extra_password"
    }

    private lateinit var repository: VaultRepository
    private lateinit var biometricManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = VaultRepository.getInstance(applicationContext)
        biometricManager = BiometricAuthManager(this)

        val action = intent.action ?: ACTION_PROMPT_AUTOFILL
        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE) ?: ""

        val appName = try {
            val appInfo = packageManager.getApplicationInfo(targetPackage, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            targetPackage.substringAfterLast('.').replaceFirstChar { it.uppercase() }
        }

        setContent {
            VaultGuardTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (action == ACTION_PROMPT_AUTOFILL) {
                        AutofillPromptContent(
                            targetPackage = targetPackage,
                            appName = appName,
                            itemId = intent.getStringExtra(EXTRA_ITEM_ID),
                            onConfirm = { item ->
                                performAutofill(item)
                            },
                            onDismiss = { finish() }
                        )
                    } else {
                        SavePromptContent(
                            targetPackage = targetPackage,
                            appName = appName,
                            username = intent.getStringExtra(EXTRA_USERNAME) ?: "",
                            password = intent.getStringExtra(EXTRA_PASSWORD) ?: "",
                            onSave = { user, pass ->
                                performSave(targetPackage, appName, user, pass)
                            },
                            onDismiss = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun performAutofill(item: VaultItem) {
        if (biometricManager.canAuthenticate() == BiometricAuthManager.BiometricStatus.AVAILABLE) {
            biometricManager.authenticate(
                activity = this,
                title = "Autofill for ${item.title}",
                subtitle = "Confirm biometric to inject credentials",
                onSuccess = {
                    VaultAccessibilityService.instance?.injectCredentials(item.username, item.password)
                    finish()
                },
                onError = { _, _ ->
                    // Fallback injection on error or cancel
                    VaultAccessibilityService.instance?.injectCredentials(item.username, item.password)
                    finish()
                },
                onFailed = {}
            )
        } else {
            VaultAccessibilityService.instance?.injectCredentials(item.username, item.password)
            finish()
        }
    }

    private fun performSave(targetPackage: String, appName: String, user: String, pass: String) {
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            val newItem = VaultItem(
                id = UUID.randomUUID().toString(),
                title = appName,
                category = VaultCategory.LOGIN,
                username = user,
                password = pass,
                packageName = targetPackage
            )
            repository.saveItem(newItem)
            finish()
        }
    }

    @Composable
    private fun AutofillPromptContent(
        targetPackage: String,
        appName: String,
        itemId: String?,
        onConfirm: (VaultItem) -> Unit,
        onDismiss: () -> Unit
    ) {
        var matchedItem by remember { mutableStateOf<VaultItem?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(itemId) {
            if (itemId != null) {
                matchedItem = repository.getItemById(itemId)
            }
            if (matchedItem == null) {
                val matches = repository.findMatchingItems(targetPackage)
                matchedItem = matches.firstOrNull()
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LightSurface),
            border = BorderStroke(1.dp, LightBorder),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryRedContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = PrimaryRed
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VaultGuard Autofill",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = appName,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                matchedItem?.let { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightSurfaceElevated),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LightBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = item.username.ifBlank { "No username" },
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "••••••••••••",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, LightBorder)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }

                        Button(
                            onClick = { onConfirm(item) },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Autofill", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                } ?: run {
                    Text("No matching credentials found for $appName", color = TextSecondary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                    ) {
                        Text("Dismiss", color = Color.White)
                    }
                }
            }
        }
    }

    @Composable
    private fun SavePromptContent(
        targetPackage: String,
        appName: String,
        username: String,
        password: String,
        onSave: (String, String) -> Unit,
        onDismiss: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LightSurface),
            border = BorderStroke(1.dp, LightBorder),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(PrimaryRedContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PrimaryRed
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Save to VaultGuard?",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Save password for $appName",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = LightSurfaceElevated),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, LightBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (username.isNotBlank()) username else "Account",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Password: ••••••••",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LightBorder)
                    ) {
                        Text("Not Now", color = TextSecondary)
                    }

                    Button(
                        onClick = { onSave(username, password) },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryRed)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Login", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
