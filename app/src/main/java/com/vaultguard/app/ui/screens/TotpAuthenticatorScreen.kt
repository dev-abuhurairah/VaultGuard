package com.vaultguard.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.BorderStroke
import com.vaultguard.app.core.model.VaultItem
import com.vaultguard.app.core.repository.VaultRepository
import com.vaultguard.app.core.security.SecureClipboardHelper
import com.vaultguard.app.features.tools.TotpGenerator
import com.vaultguard.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TotpAuthenticatorScreen(
    repository: VaultRepository,
    onEditItem: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardHelper = remember { SecureClipboardHelper(context) }
    val allItems by repository.getAllItemsFlow().collectAsState(initial = emptyList())

    val totpItems = remember(allItems) {
        allItems.filter { it.totpSecret.isNotBlank() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBg)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "2FA Authenticator",
            color = TextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Time-based one-time passwords refreshed every 30 seconds",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        if (totpItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LockClock,
                        contentDescription = null,
                        tint = LightBorder,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No 2FA accounts added",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Add a 2FA secret key to any item in your vault to generate codes",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(totpItems, key = { it.id }) { item ->
                    TotpCard(
                        item = item,
                        onClick = { onEditItem(item.id) },
                        onCopyCode = { code ->
                            clipboardHelper.copySensitiveText(item.title, code)
                            Toast.makeText(context, "2FA Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TotpCard(
    item: VaultItem,
    onClick: () -> Unit,
    onCopyCode: (String) -> Unit
) {
    var totpState by remember { mutableStateOf<TotpGenerator.TotpState?>(null) }

    LaunchedEffect(item.totpSecret) {
        while (true) {
            totpState = TotpGenerator.generateCurrentTotp(item.totpSecret)
            delay(1000)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = LightSurface),
        border = BorderStroke(1.dp, LightBorder),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.username.isNotBlank()) {
                        Text(
                            text = item.username,
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                totpState?.let { totp ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            progress = { totp.progress },
                            modifier = Modifier.size(24.dp),
                            color = if (totp.remainingSeconds <= 5) PrimaryRedDark else PrimaryRed,
                            strokeWidth = 3.dp,
                            trackColor = Color(0xFFE2E8F0)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${totp.remainingSeconds}s",
                            color = if (totp.remainingSeconds <= 5) PrimaryRedDark else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            totpState?.let { totp ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val formattedCode = if (totp.code.length == 6) {
                        "${totp.code.substring(0, 3)} ${totp.code.substring(3)}"
                    } else {
                        totp.code
                    }

                    Text(
                        text = formattedCode,
                        color = PrimaryRed,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )

                    IconButton(
                        onClick = { onCopyCode(totp.code) },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PrimaryRedContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy 2FA Code",
                            tint = PrimaryRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } ?: run {
                Text(
                    text = "Invalid 2FA Secret Key",
                    color = PrimaryRed,
                    fontSize = 14.sp
                )
            }
        }
    }
}
