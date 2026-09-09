package com.vaultguard.app.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.vaultguard.app.core.model.VaultItem
import com.vaultguard.app.core.repository.VaultRepository
import com.vaultguard.app.ui.PromptDialogActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Real-time Accessibility Engine for deep input field detection.
 * Provides fallback detection and floating confirmation prompts across all apps & browsers.
 */
class VaultAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var repository: VaultRepository
    private var lastPromptTime: Long = 0
    private var detectionJob: Job? = null

    // Track recently typed credentials for auto-save detection
    private var lastEnteredUsername: String? = null
    private var lastEnteredPassword: String? = null
    private var lastPackageName: String? = null

    companion object {
        @Volatile
        var instance: VaultAccessibilityService? = null
            private set

        const val ACTION_FILL = "com.vaultguard.action.FILL"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_PASSWORD = "extra_password"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        repository = VaultRepository.getInstance(applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return

        // Ignore our own app
        if (packageName == applicationContext.packageName) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                handleFieldFocus(packageName, event.source)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                trackTextChange(packageName, event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleSubmitClick(packageName, event.source)
            }
        }
    }

    private fun handleFieldFocus(packageName: String, source: AccessibilityNodeInfo?) {
        if (source == null) return
        val isPassword = isPasswordNode(source)
        val isUsername = isUsernameNode(source)

        if (!isPassword && !isUsername) return

        val now = System.currentTimeMillis()
        if (now - lastPromptTime < 3000) return // Throttle prompts

        detectionJob?.cancel()
        detectionJob = serviceScope.launch {
            delay(300) // Debounce focus
            val matchingItems = repository.findMatchingItems(packageName)
            if (matchingItems.isNotEmpty()) {
                lastPromptTime = System.currentTimeMillis()
                showAutofillPrompt(packageName, matchingItems)
            }
        }
    }

    private fun trackTextChange(packageName: String, event: AccessibilityEvent) {
        val source = event.source ?: return
        val text = event.text.joinToString("")

        if (isPasswordNode(source)) {
            lastEnteredPassword = text
            lastPackageName = packageName
        } else if (isUsernameNode(source)) {
            lastEnteredUsername = text
            lastPackageName = packageName
        }
    }

    private fun handleSubmitClick(packageName: String, source: AccessibilityNodeInfo?) {
        if (source == null) return
        val text = source.text?.toString()?.lowercase() ?: ""
        val isSubmitButton = text.contains("login") || text.contains("sign in") ||
                text.contains("submit") || text.contains("continue") || text.contains("log in")

        if (isSubmitButton && !lastEnteredPassword.isNullOrBlank() && lastPackageName == packageName) {
            val passToSave = lastEnteredPassword!!
            val userToSave = lastEnteredUsername ?: ""
            showSavePrompt(packageName, userToSave, passToSave)

            // Reset tracked credentials
            lastEnteredPassword = null
            lastEnteredUsername = null
        }
    }

    private fun showAutofillPrompt(packageName: String, items: List<VaultItem>) {
        val intent = Intent(this, PromptDialogActivity::class.java).apply {
            action = PromptDialogActivity.ACTION_PROMPT_AUTOFILL
            putExtra(PromptDialogActivity.EXTRA_TARGET_PACKAGE, packageName)
            putExtra(PromptDialogActivity.EXTRA_ITEM_ID, items.first().id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun showSavePrompt(packageName: String, username: String, pass: String) {
        val intent = Intent(this, PromptDialogActivity::class.java).apply {
            action = PromptDialogActivity.ACTION_PROMPT_SAVE
            putExtra(PromptDialogActivity.EXTRA_TARGET_PACKAGE, packageName)
            putExtra(PromptDialogActivity.EXTRA_USERNAME, username)
            putExtra(PromptDialogActivity.EXTRA_PASSWORD, pass)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    /**
     * Injects credentials into the active window nodes.
     */
    fun injectCredentials(username: String, password: String) {
        val rootNode = rootInActiveWindow ?: return
        serviceScope.launch {
            findAndFillNode(rootNode, username, isUser = true)
            findAndFillNode(rootNode, password, isUser = false)
        }
    }

    private fun findAndFillNode(node: AccessibilityNodeInfo, text: String, isUser: Boolean): Boolean {
        if (isUser && isUsernameNode(node)) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            return true
        }

        if (!isUser && isPasswordNode(node)) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndFillNode(child, text, isUser)) {
                return true
            }
        }
        return false
    }

    private fun isPasswordNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        val hint = (node.hintText?.toString() ?: "").lowercase()
        val text = (node.text?.toString() ?: "").lowercase()
        val viewId = (node.viewIdResourceName ?: "").lowercase()

        return viewId.contains("password") || viewId.contains("pwd") || viewId.contains("pin") ||
                hint.contains("password") || hint.contains("pin") || text.contains("password")
    }

    private fun isUsernameNode(node: AccessibilityNodeInfo): Boolean {
        val hint = (node.hintText?.toString() ?: "").lowercase()
        val viewId = (node.viewIdResourceName ?: "").lowercase()

        return viewId.contains("user") || viewId.contains("email") || viewId.contains("phone") ||
                hint.contains("user") || hint.contains("email") || hint.contains("username")
    }

    override fun onInterrupt() {
        detectionJob?.cancel()
    }
}
