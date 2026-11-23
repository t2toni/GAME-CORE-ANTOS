package com.antos.gaming

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.antos.gaming.monitoring.ScreenCaptureService
import com.antos.gaming.service.OverlayService
import com.antos.gaming.ui.dashboard.AppPickerScreen
import com.antos.gaming.ui.dashboard.DashboardScreen
import com.antos.gaming.ui.dashboard.DashboardViewModel
import com.antos.gaming.ui.settings.PermissionCenterScreen
import com.antos.gaming.ui.settings.SettingsScreen
import com.antos.gaming.ui.theme.AntosTheme
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {

    private val screenCaptureLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == RESULT_OK) {

                val data = result.data ?: return@registerForActivityResult

                val serviceIntent =
                    Intent(
                        this,
                        ScreenCaptureService::class.java
                    ).apply {

                        action =
                            ScreenCaptureService.ACTION_START

                        putExtra(
                            ScreenCaptureService.EXTRA_RESULT_CODE,
                            result.resultCode
                        )

                        putExtra(
                            ScreenCaptureService.EXTRA_DATA,
                            data
                        )
                    }

                startForegroundService(
                    serviceIntent
                )
            }
        }

    fun requestScreenCapture() {

        val manager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        screenCaptureLauncher.launch(
            manager.createScreenCaptureIntent()
        )
    }

    fun stopScreenCapture() {
        val serviceIntent =
            Intent(
                this,
                ScreenCaptureService::class.java
            ).apply {
                action = ScreenCaptureService.ACTION_STOP
            }
        startService(serviceIntent)
    }

    fun startGamingOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            )
            startActivity(intent)
            return
        }

        val serviceIntent =
            Intent(
                this,
                OverlayService::class.java
            )
        startForegroundService(serviceIntent)
    }

    private val closeReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            finishAffinity()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = android.content.IntentFilter(OverlayService.ACTION_CLOSE_ANTOS_APP)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(closeReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, filter)
        }

        setContent {
            AntosTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            val dashboardViewModel: DashboardViewModel = viewModel()
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToPermissions = { navController.navigate("permissions") },
                                onNavigateToAddGame = { navController.navigate("app_picker") }
                            )
                        }
                        composable("app_picker") {
                            AppPickerScreen(onNavigateBack = { navController.popBackStack() })
                        }
                        composable("settings") {
                            SettingsScreen(onBack = { navController.popBackStack() })
                        }
                        composable("permissions") {
                            PermissionCenterScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(closeReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
        }
    }
}

