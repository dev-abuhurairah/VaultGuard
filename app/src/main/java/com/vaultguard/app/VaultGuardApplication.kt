package com.vaultguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.vaultguard.app.core.repository.VaultRepository

class VaultGuardApplication : Application() {

    lateinit var repository: VaultRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = VaultRepository.getInstance(this)
        createNotificationChannels()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                repository.checkAutoLock()
            }

            override fun onStop(owner: LifecycleOwner) {
                repository.touch()
            }
        })
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val promptChannel = NotificationChannel(
                CHANNEL_PROMPTS,
                getString(R.string.channel_prompt_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_prompt_desc)
                enableVibration(true)
            }

            val clipboardChannel = NotificationChannel(
                CHANNEL_CLIPBOARD,
                getString(R.string.channel_clipboard_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_clipboard_desc)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(promptChannel)
            manager?.createNotificationChannel(clipboardChannel)
        }
    }

    companion object {
        const val CHANNEL_PROMPTS = "vaultguard_prompts"
        const val CHANNEL_CLIPBOARD = "vaultguard_clipboard"
    }
}
