package com.vaultguard.app.core.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Secure Clipboard Manager.
 * 
 * Features:
 * 1. Sets ClipDescription.EXTRA_IS_SENSITIVE on Android 13+ to prevent system preview leakage.
 * 2. Auto-clears the clipboard after a designated timeout (default 30 seconds).
 */
class SecureClipboardHelper(private val context: Context) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val scope = CoroutineScope(Dispatchers.Main)
    private var clearJob: Job? = null

    /**
     * Copies sensitive text (passwords, 2FA codes) safely to the system clipboard.
     */
    fun copySensitiveText(label: String, text: String, autoClearSeconds: Int = 30) {
        val clip = ClipData.newPlainText(label, text)

        // Mark as sensitive on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        clipboardManager.setPrimaryClip(clip)

        // Schedule auto-clearing
        scheduleAutoClear(text, autoClearSeconds)
    }

    private fun scheduleAutoClear(copiedText: String, timeoutSeconds: Int) {
        clearJob?.cancel()
        if (timeoutSeconds <= 0) return

        clearJob = scope.launch {
            delay(timeoutSeconds * 1000L)
            
            // Only clear if the user hasn't already copied something else
            val currentClip = clipboardManager.primaryClip
            if (currentClip != null && currentClip.itemCount > 0) {
                val currentText = currentClip.getItemAt(0).text?.toString()
                if (currentText == copiedText) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboardManager.clearPrimaryClip()
                    } else {
                        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            }
        }
    }
}
