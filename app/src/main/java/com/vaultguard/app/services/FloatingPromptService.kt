package com.vaultguard.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.vaultguard.app.MainActivity
import com.vaultguard.app.R

/**
 * Foreground Service that manages floating overlay prompts on top of other apps.
 */
class FloatingPromptService : Service() {

    companion object {
        const val CHANNEL_ID = "vaultguard_floating_prompt"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SHOW_OVERLAY = "com.vaultguard.action.SHOW_OVERLAY"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_USERNAME = "extra_username"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SHOW_OVERLAY) {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: "VaultGuard"
            val username = intent.getStringExtra(EXTRA_USERNAME) ?: ""
            showFloatingPrompt(title, username)
        }
        return START_NOT_STICKY
    }

    private fun showFloatingPrompt(title: String, username: String) {
        if (overlayView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            y = 100
        }

        // Programmatic lightweight UI view for the overlay
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xFF0F172A.toInt())
            setPadding(48, 36, 48, 36)
            elevation = 16f
        }

        val titleTv = TextView(this).apply {
            text = "VaultGuard Autofill: $title"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val userTv = TextView(this).apply {
            text = if (username.isNotBlank()) "Fill credentials for $username?" else "Fill saved credentials?"
            setTextColor(0xFF94A3B8.toInt())
            textSize = 14f
            setPadding(0, 8, 0, 16)
        }

        val buttonLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
        }

        val confirmBtn = Button(this).apply {
            text = "Confirm Autofill"
            setBackgroundColor(0xFF059669.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            setOnClickListener {
                removeFloatingPrompt()
                // Trigger fill through accessibility service
            }
        }

        val dismissBtn = Button(this).apply {
            text = "Dismiss"
            setBackgroundColor(0x00000000)
            setTextColor(0xFF94A3B8.toInt())
            setOnClickListener {
                removeFloatingPrompt()
            }
        }

        buttonLayout.addView(confirmBtn)
        buttonLayout.addView(dismissBtn)
        layout.addView(titleTv)
        layout.addView(userTv)
        layout.addView(buttonLayout)

        overlayView = layout
        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeFloatingPrompt() {
        overlayView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VaultGuard Service Active")
            .setContentText("Monitoring password autofill and auto-save requests")
            .setSmallIcon(R.drawable.ic_shield_vault)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VaultGuard Autofill Assistant",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingPrompt()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
