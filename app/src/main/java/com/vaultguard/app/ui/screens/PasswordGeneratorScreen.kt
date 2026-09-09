package com.vaultguard.app.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaultguard.app.core.security.SecureClipboardHelper
import com.vaultguard.app.features.tools.PasswordGenerator

@Composable
fun PasswordGeneratorScreen() {
    val context = LocalContext.current
    val clipboardHelper = remember { SecureClipboardHelper(context) }
    val scrollState = rememberScrollState()

    var length by remember { mutableFloatStateOf(16f) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeDigits by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(true) }
    var isPassphrase by remember { mutableStateOf(false) }
    var wordCount by remember { mutableFloatStateOf(4f) }

    fun generateCurrentPassword(): String {
        return PasswordGenerator.generate(
            PasswordGenerator.GeneratorConfig(
                length = length.toInt(),
                includeUppercase = includeUppercase,
                includeLowercase = includeLowercase,
                includeDigits = includeDigits,
                includeSymbols = includeSymbols,
                excludeAmbiguous = excludeAmbiguous,
                isPassphrase = isPassphrase,
                wordCount = wordCount.toInt()
            )
        )
    }

    var generatedPassword by remember { mutableStateOf(generateCurrentPassword()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Password Generator",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Generate strong, cryptographically secure passwords or passphrases",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        // Password Display Box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = generatedPassword,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Entropy & Strength Bar
                val strength = PasswordGenerator.evaluateStrength(generatedPassword)
                val entropy = PasswordGenerator.calculateEntropy(generatedPassword)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strength.label} (${entropy.toInt()} bits)",
                        color = strength.toComposeColor(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )

                    Row {
                        IconButton(
                            onClick = { generatedPassword = generateCurrentPassword() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate", tint = Color(0xFF10B981))
                        }

                        IconButton(
                            onClick = {
                                clipboardHelper.copySensitiveText("Generated Password", generatedPassword)
                                Toast.makeText(context, "Password copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF38BDF8))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (strength.score + 1) / 5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = strength.toComposeColor(),
                    trackColor = Color(0xFF1E293B)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mode: Password vs Passphrase
        GeneratorToggleRow(
            title = "Passphrase Mode (Diceware)",
            subtitle = "Generate memorable multi-word phrase",
            checked = isPassphrase,
            onCheckedChange = {
                isPassphrase = it
                generatedPassword = generateCurrentPassword()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isPassphrase) {
            Text(
                text = "Word Count: ${wordCount.toInt()} words",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Slider(
                value = wordCount,
                onValueChange = {
                    wordCount = it
                    generatedPassword = generateCurrentPassword()
                },
                valueRange = 3f..8f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF059669),
                    activeTrackColor = Color(0xFF059669),
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )
        } else {
            Text(
                text = "Password Length: ${length.toInt()} characters",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
            Slider(
                value = length,
                onValueChange = {
                    length = it
                    generatedPassword = generateCurrentPassword()
                },
                valueRange = 8f..64f,
                steps = 55,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF059669),
                    activeTrackColor = Color(0xFF059669),
                    inactiveTrackColor = Color(0xFF1E293B)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            GeneratorToggleRow(
                title = "Uppercase Letters (A-Z)",
                checked = includeUppercase,
                onCheckedChange = {
                    includeUppercase = it
                    generatedPassword = generateCurrentPassword()
                }
            )

            GeneratorToggleRow(
                title = "Lowercase Letters (a-z)",
                checked = includeLowercase,
                onCheckedChange = {
                    includeLowercase = it
                    generatedPassword = generateCurrentPassword()
                }
            )

            GeneratorToggleRow(
                title = "Numbers (0-9)",
                checked = includeDigits,
                onCheckedChange = {
                    includeDigits = it
                    generatedPassword = generateCurrentPassword()
                }
            )

            GeneratorToggleRow(
                title = "Special Characters (!@#$%)",
                checked = includeSymbols,
                onCheckedChange = {
                    includeSymbols = it
                    generatedPassword = generateCurrentPassword()
                }
            )

            GeneratorToggleRow(
                title = "Exclude Ambiguous (0/O, 1/l)",
                checked = excludeAmbiguous,
                onCheckedChange = {
                    excludeAmbiguous = it
                    generatedPassword = generateCurrentPassword()
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                clipboardHelper.copySensitiveText("Generated Password", generatedPassword)
                Toast.makeText(context, "Password copied to clipboard (clears in 30s)", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copy Password", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun GeneratorToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
        Switch(
            checked = checked,
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
