package com.antos.gaming.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.antos.gaming.permissions.PermissionManager
import com.antos.gaming.service.GamingModeManager
import com.antos.gaming.ui.theme.VioletMain
import com.antos.gaming.ui.theme.NearBlack
import androidx.compose.ui.Alignment

@Composable
fun PermissionCenterScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val gamingModeManager = GamingModeManager(context)
    val hasOverlay = PermissionManager.hasOverlayPermission(context)
    val hasDnd = gamingModeManager.hasDndPermission()
    val hasWriteSettings = PermissionManager.hasWriteSettingsPermission(context)
    val hasUsageStats = PermissionManager.hasUsageStatsPermission(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NearBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PERMISSIONS",
                style = MaterialTheme.typography.displayMedium,
                color = VioletMain
            )
            Button(onClick = onBack) {
                Text("BACK")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        PermissionItem(
            title = "OVERLAY",
            isGranted = hasOverlay,
            onClick = {
                context.startActivity(PermissionManager.getOverlayPermissionIntent(context))
            }
        )
        
        PermissionItem(
            title = "NOTIFICATION POLICY (DND)",
            isGranted = hasDnd,
            onClick = {
                context.startActivity(gamingModeManager.getDndPermissionIntent())
            }
        )
        
        PermissionItem(
            title = "MODIFY SYSTEM SETTINGS",
            isGranted = hasWriteSettings,
            onClick = {
                context.startActivity(PermissionManager.getWriteSettingsPermissionIntent(context))
            }
        )

        PermissionItem(
            title = "USAGE ACCESS (AUTO-CLOSE ON GAME EXIT)",
            isGranted = hasUsageStats,
            onClick = {
                context.startActivity(PermissionManager.getUsageStatsPermissionIntent(context))
            }
        )
    }

}

@Composable
fun PermissionItem(title: String, isGranted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = title, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = if (isGranted) "✓ Granted" else "Not granted", 
                color = if (isGranted) com.antos.gaming.ui.theme.HudAccent else MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium
            )
        }
        if (!isGranted) {
            Button(onClick = onClick) {
                Text("ALLOW")
            }
        }
    }
}
