package com.vaultguard.app.ui.screens

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.vaultguard.app.core.repository.VaultRepository
import com.vaultguard.app.core.security.BiometricAuthManager
import com.vaultguard.app.features.tools.PasswordGenerator
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.OutlinedButton
import com.vaultguard.app.ui.theme.*

@Composable
fun MasterLockScreen(
    repository: VaultRepository,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val isSetup = remember { repository.isVaultSetup() }

    val coroutineScope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val biometricManager = remember { BiometricAuthManager(context) }
    val isBiometricAvailable = remember {
        biometricManager.canAuthenticate() == BiometricAuthManager.BiometricStatus.AVAILABLE
    }

    // Auto-trigger biometric prompt on launch if vault is setup & biometric key is synced
    LaunchedEffect(Unit) {
        if (isSetup && isBiometricAvailable && repository.isBiometricKeySynced()) {
            (context as? FragmentActivity)?.let { activity ->
                biometricManager.authenticate(
                    activity = activity,
                    title = "Unlock VaultGuard",
                    subtitle = "Verify your fingerprint or face",
                    onSuccess = {
                        if (repository.unlockWithBiometric()) {
                            onUnlocked()
                        }
                    },
                    onError = { _, _ -> },
                    onFailed = {}
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Vault Shield Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(PrimaryRedContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = PrimaryRed,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isSetup) "Unlock Vault" else "Create Master Password",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (isSetup)
                "Enter your master password to decrypt your vault"
            else
                "This password encrypts all your credentials on this device. Do not forget it.",
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        // Master Password Field
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
            label = { Text("Master Password") },
            leadingIcon = {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary)
            },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (!isSetup) ImeAction.Next else ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (isSetup && !isLoading && password.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val success = repository.unlockWithPassword(password)
                            isLoading = false
                            if (success) {
                                onUnlocked()
                            } else {
                                errorMessage = "Incorrect master password. Please try again."
                            }
                        }
                    }
                }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryRed,
                unfocusedBorderColor = LightBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = PrimaryRed,
                unfocusedLabelColor = TextSecondary,
                focusedContainerColor = LightSurface,
                unfocusedContainerColor = LightSurface
            ),
            shape = RoundedCornerShape(14.dp)
        )

        // If First-Time Setup: Strength Bar & Confirm Field
        if (!isSetup) {
            Spacer(modifier = Modifier.height(10.dp))

            val strength = PasswordGenerator.evaluateStrength(password)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Password Strength", color = TextSecondary, fontSize = 12.sp)
                    Text(
                        text = strength.label,
                        color = strength.toComposeColor(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (strength.score + 1) / 5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = strength.toComposeColor(),
                    trackColor = Color(0xFFE2E8F0)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text("Confirm Master Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary)
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryRed,
                    unfocusedBorderColor = LightBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedLabelColor = PrimaryRed,
                    unfocusedLabelColor = TextSecondary,
                    focusedContainerColor = LightSurface,
                    unfocusedContainerColor = LightSurface
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryRedContainer),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PrimaryRed.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = PrimaryRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = error,
                        color = PrimaryRedOnContainer,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main Action Button (with non-blocking coroutines and loading spinner)
        Button(
            onClick = {
                if (isLoading) return@Button
                errorMessage = null
                if (isSetup) {
                    if (password.isBlank()) {
                        errorMessage = "Please enter your master password."
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        val success = repository.unlockWithPassword(password)
                        isLoading = false
                        if (success) {
                            onUnlocked()
                        } else {
                            errorMessage = "Incorrect master password. Please try again."
                        }
                    }
                } else {
                    if (password.length < 8) {
                        errorMessage = "Password must be at least 8 characters long."
                    } else if (password != confirmPassword) {
                        errorMessage = "Passwords do not match."
                    } else {
                        isLoading = true
                        coroutineScope.launch {
                            val success = repository.setupMasterPassword(password)
                            isLoading = false
                            if (success) {
                                onUnlocked()
                            } else {
                                errorMessage = "Failed to initialize vault encryption. Please try again."
                            }
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryRed,
                disabledContainerColor = PrimaryRedLight.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isSetup) "Decrypting Vault..." else "Setting up Encryption...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = if (isSetup) "Unlock Vault" else "Create Vault",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Biometric Quick Unlock Option
        if (isSetup && isBiometricAvailable) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    if (isLoading) return@OutlinedButton
                    errorMessage = null
                    (context as? FragmentActivity)?.let { activity ->
                        biometricManager.authenticate(
                            activity = activity,
                            title = "Unlock VaultGuard",
                            subtitle = "Verify your fingerprint or face",
                            onSuccess = {
                                if (repository.unlockWithBiometric()) {
                                    onUnlocked()
                                } else {
                                    errorMessage = "Biometrics not synced yet. Enter master password once to activate."
                                }
                            },
                            onError = { code, msg ->
                                // Ignore cancellation by user
                                if (code != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                                    code != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                                    code != androidx.biometric.BiometricPrompt.ERROR_CANCELED
                                ) {
                                    errorMessage = msg.toString()
                                }
                            },
                            onFailed = {
                                errorMessage = "Biometric not recognized. Please try again or use password."
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = LightSurface,
                    contentColor = TextPrimary
                ),
                border = BorderStroke(1.dp, LightBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Unlock",
                    tint = PrimaryRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Unlock with Biometrics",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
    }
}
