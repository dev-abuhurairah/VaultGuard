package com.vaultguard.app.services

import android.app.assist.AssistStructure
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillContext
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.vaultguard.app.R
import com.vaultguard.app.core.model.VaultCategory
import com.vaultguard.app.core.model.VaultItem
import com.vaultguard.app.core.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Native Android Autofill Framework Service (Android 8.0+ / 13+ / 14+).
 * Handles onFillRequest to present inline/popup suggestions and onSaveRequest to prompt saving credentials.
 */
class VaultAutofillService : AutofillService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: VaultRepository

    override fun onCreate() {
        super.onCreate()
        repository = VaultRepository.getInstance(applicationContext)
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val fillContexts = request.fillContexts
        if (fillContexts.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val structure = fillContexts.last().structure
        val packageName = structure.activityComponent.packageName

        serviceScope.launch {
            try {
                val parsedFields = parseStructure(structure)
                if (parsedFields.passwordId == null && parsedFields.usernameId == null) {
                    callback.onSuccess(null)
                    return@launch
                }

                // Match against package name and web domain
                val target = parsedFields.webDomain ?: packageName
                val matchingItems = repository.findMatchingItems(target)

                if (matchingItems.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }

                val responseBuilder = FillResponse.Builder()

                for (item in matchingItems) {
                    val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_2).apply {
                        setTextViewText(android.R.id.text1, item.title)
                        setTextViewText(android.R.id.text2, item.username.ifBlank { "VaultGuard Credential" })
                    }

                    val datasetBuilder = Dataset.Builder(presentation)

                    parsedFields.usernameId?.let { userNodeId ->
                        datasetBuilder.setValue(userNodeId, AutofillValue.forText(item.username))
                    }

                    parsedFields.passwordId?.let { passNodeId ->
                        datasetBuilder.setValue(passNodeId, AutofillValue.forText(item.password))
                    }

                    responseBuilder.addDataset(datasetBuilder.build())
                }

                // Setup SaveInfo so system triggers onSaveRequest on new inputs
                val saveTypes = SaveInfo.SAVE_DATA_TYPE_PASSWORD or SaveInfo.SAVE_DATA_TYPE_USERNAME
                val requiredIds = listOfNotNull(parsedFields.usernameId, parsedFields.passwordId).toTypedArray()

                if (requiredIds.isNotEmpty()) {
                    val saveInfo = SaveInfo.Builder(saveTypes, requiredIds)
                        .build()
                    responseBuilder.setSaveInfo(saveInfo)
                }

                callback.onSuccess(responseBuilder.build())
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onSuccess(null)
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val fillContexts = request.fillContexts
        if (fillContexts.isEmpty()) {
            callback.onSuccess()
            return
        }

        val structure = fillContexts.last().structure
        val packageName = structure.activityComponent.packageName

        serviceScope.launch {
            try {
                val parsed = parseStructure(structure)
                if (!parsed.savedPassword.isNullOrBlank()) {
                    val username = parsed.savedUsername ?: ""
                    val title = parsed.webDomain ?: packageName.substringAfterLast('.')

                    val newItem = VaultItem(
                        id = UUID.randomUUID().toString(),
                        title = title.replaceFirstChar { it.uppercase() },
                        category = VaultCategory.LOGIN,
                        username = username,
                        password = parsed.savedPassword,
                        packageName = packageName,
                        websiteUrl = parsed.webDomain ?: ""
                    )

                    if (repository.isUnlocked.value) {
                        repository.saveItem(newItem)
                    }
                }
                callback.onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onSuccess()
            }
        }
    }

    data class ParsedFields(
        var usernameId: AutofillId? = null,
        var passwordId: AutofillId? = null,
        var savedUsername: String? = null,
        var savedPassword: String? = null,
        var webDomain: String? = null
    )

    private fun parseStructure(structure: AssistStructure): ParsedFields {
        val result = ParsedFields()
        val nodeCount = structure.windowNodeCount
        for (i in 0 until nodeCount) {
            val windowNode = structure.getWindowNodeAt(i)
            traverseNode(windowNode.rootViewNode, result)
        }
        return result
    }

    private fun traverseNode(node: AssistStructure.ViewNode, result: ParsedFields) {
        val webDomain = node.webDomain
        if (!webDomain.isNullOrBlank()) {
            result.webDomain = webDomain
        }

        val hints = node.autofillHints
        val hintText = node.hint?.toString()?.lowercase() ?: ""
        val idEntry = node.idEntry?.lowercase() ?: ""

        val isPass = (hints != null && hints.any { it.contains("password", ignoreCase = true) }) ||
                node.className?.contains("password", ignoreCase = true) == true ||
                hintText.contains("password") || hintText.contains("pin") ||
                idEntry.contains("password") || idEntry.contains("pin")

        val isUser = (hints != null && hints.any { it.contains("username", ignoreCase = true) || it.contains("email", ignoreCase = true) }) ||
                hintText.contains("username") || hintText.contains("email") ||
                idEntry.contains("username") || idEntry.contains("email")

        if (isPass && result.passwordId == null) {
            result.passwordId = node.autofillId
            result.savedPassword = node.text?.toString()
        } else if (isUser && result.usernameId == null) {
            result.usernameId = node.autofillId
            result.savedUsername = node.text?.toString()
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i), result)
        }
    }
}
