package com.antos.gaming.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antos.gaming.permissions.PermissionManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            // Note: In a real app, we should check DataStore to see if "Start on boot" is enabled.
            // For now, if we have overlay permission, we start the service.
            if (PermissionManager.hasOverlayPermission(context)) {
                val serviceIntent = Intent(context, OverlayService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
