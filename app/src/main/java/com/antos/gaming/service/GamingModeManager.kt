package com.antos.gaming.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.antos.gaming.permissions.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GamingModeManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val _isGamingModeActive = MutableStateFlow(false)
    val isGamingModeActive: StateFlow<Boolean> = _isGamingModeActive.asStateFlow()

    fun hasDndPermission(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    fun getDndPermissionIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun toggleGamingMode() {
        val newState = !_isGamingModeActive.value
        
        if (newState) {
            if (hasDndPermission()) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        } else {
            if (hasDndPermission()) {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        }
        
        _isGamingModeActive.value = newState
    }

    fun toggleRotationLock() {
        if (!PermissionManager.hasWriteSettingsPermission(context)) return
        
        try {
            val contentResolver = context.contentResolver
            val currentRotation = Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            val newRotation = if (currentRotation == 1) 0 else 1
            Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, newRotation)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setMaxBrightness() {
        if (!PermissionManager.hasWriteSettingsPermission(context)) return
        
        try {
            val contentResolver = context.contentResolver
            // Disable auto brightness first
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            // Set max brightness (usually 255)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 255)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
