package com.antos.gaming.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import com.antos.gaming.ui.theme.VioletMain
import com.antos.gaming.ui.theme.NearBlack
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.antos.gaming.settings.SettingsManager

@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()
    
    val startOnBoot by settingsManager.startOnBootFlow.collectAsState(initial = false)
    val autoGamingMode by settingsManager.autoGamingModeFlow.collectAsState(initial = false)
    val enableSidebar by settingsManager.enableSidebarFlow.collectAsState(initial = true)
    val autoHide by settingsManager.autoHideFlow.collectAsState(initial = false)
    val showFpsCounter by settingsManager.showFpsCounterFlow.collectAsState(initial = false)

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
                text = "SETTINGS",
                style = MaterialTheme.typography.displayMedium,
                color = VioletMain
            )
            Button(onClick = onBack) {
                Text("BACK")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("GENERAL", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            title = "Start ANTOS on boot",
            isChecked = startOnBoot,
            onCheckedChange = { scope.launch { settingsManager.saveSetting(SettingsManager.START_ON_BOOT, it) } }
        )
        SettingsItem(
            title = "Auto gaming mode",
            isChecked = autoGamingMode,
            onCheckedChange = { scope.launch { settingsManager.saveSetting(SettingsManager.AUTO_GAMING_MODE, it) } }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("OVERLAY", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsItem(
            title = "Enable sidebar",
            isChecked = enableSidebar,
            onCheckedChange = { scope.launch { settingsManager.saveSetting(SettingsManager.ENABLE_SIDEBAR, it) } }
        )
        SettingsItem(
            title = "Auto-hide",
            isChecked = autoHide,
            onCheckedChange = { scope.launch { settingsManager.saveSetting(SettingsManager.AUTO_HIDE, it) } }
        )
        SettingsItem(
            title = "Show FPS Counter",
            isChecked = showFpsCounter,
            onCheckedChange = { scope.launch { settingsManager.saveSetting(SettingsManager.SHOW_FPS_COUNTER, it) } }
        )
    }
}

@Composable
fun SettingsItem(title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, color = MaterialTheme.colorScheme.onBackground)
        Switch(
            checked = isChecked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = VioletMain, checkedTrackColor = VioletMain.copy(alpha = 0.5f))
        )
    }
}
