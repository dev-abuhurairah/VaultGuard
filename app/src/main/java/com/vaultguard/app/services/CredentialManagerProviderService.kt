package com.vaultguard.app.services

import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CredentialProviderService
import com.vaultguard.app.core.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Modern Android 14+ (API 34+) Credential Provider Service.
 * Integrates with Android Credential Manager for Passkeys, Passwords, and federated sign-in.
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class CredentialManagerProviderService : CredentialProviderService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var repository: VaultRepository

    override fun onCreate() {
        super.onCreate()
        repository = VaultRepository.getInstance(applicationContext)
    }

    override fun onBeginGetCredential(
        request: BeginGetCredentialRequest,
        cancellationSignal: CancellationSignal,
        callback: androidx.credentials.provider.BeginGetCredentialResponseCallback
    ) {
        serviceScope.launch {
            try {
                val callingPackage = request.callingAppInfo?.packageName ?: ""
                val items = repository.findMatchingItems(callingPackage)

                val responseBuilder = BeginGetCredentialResponse.Builder()
                // In full implementation, entries can be populated with PasswordCredentialEntry
                callback.onResult(responseBuilder.build())
            } catch (e: Exception) {
                callback.onError(androidx.credentials.exceptions.GetCredentialUnknownException(e.message))
            }
        }
    }

    override fun onClearCredentialState(
        request: androidx.credentials.provider.ClearCredentialStateRequest,
        cancellationSignal: CancellationSignal,
        callback: androidx.credentials.provider.ClearCredentialStateCallback
    ) {
        callback.onResult()
    }
}
