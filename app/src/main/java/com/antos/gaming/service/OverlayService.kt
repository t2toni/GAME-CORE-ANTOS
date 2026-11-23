package com.antos.gaming.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.antos.gaming.MainActivity
import com.antos.gaming.monitoring.MonitoringManager
import com.antos.gaming.monitoring.ScreenCaptureService
import com.antos.gaming.permissions.PermissionManager
import com.antos.gaming.ui.sidebar.OverlayComposeView
import com.antos.gaming.ui.theme.AntosTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    companion object {
        const val EXTRA_GAME_PACKAGE = "extra_game_package"
        const val ACTION_STOP_SERVICE = "com.antos.gaming.action.STOP_OVERLAY"
        const val ACTION_CLOSE_ANTOS_APP = "com.antos.gaming.action.CLOSE_APP"
        private const val CHANNEL_ID = "antos_gaming_channel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private lateinit var monitoringManager: MonitoringManager

    // Lifecycle requirements for ComposeView
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    // Game lifecycle monitoring
    private var currentGamePackage: String? = null
    private var gameMonitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        monitoringManager = MonitoringManager.getInstance(this)
        monitoringManager.startMonitoring()

        startForegroundService()
        showOverlay()

        lifecycleScope.launch {
            val settingsManager = com.antos.gaming.settings.SettingsManager(this@OverlayService)
            kotlinx.coroutines.flow.combine(
                settingsManager.showNormalFpsFlow,
                settingsManager.showAccurateFpsFlow,
                settingsManager.showFpsCounterFlow
            ) { normal, accurate, legacy ->
                normal || accurate || legacy
            }.collect { show ->
                if (show) {
                    showFpsOverlay()
                } else {
                    hideFpsOverlay()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            closeAllAndStop()
            return START_NOT_STICKY
        }

        val pkg = intent?.getStringExtra(EXTRA_GAME_PACKAGE)
        if (!pkg.isNullOrBlank()) {
            currentGamePackage = pkg
            updateNotification("Game active: $pkg")
            startGameMonitoring(pkg)
        }

        return START_STICKY
    }

    private fun startGameMonitoring(gamePackage: String) {
        gameMonitorJob?.cancel()
        gameMonitorJob = lifecycleScope.launch(Dispatchers.Default) {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val launcherPackages = getLauncherPackages()
            var consecutiveAwaySeconds = 0

            // Initial brief delay to allow the game activity to come to foreground
            delay(2500)

            while (isActive) {
                delay(1000)

                val topPackage = getForegroundPackage(usageStatsManager)
                if (topPackage != null) {
                    val isGame = topPackage == gamePackage
                    val isOurApp = topPackage == packageName
                    val isLauncher = launcherPackages.contains(topPackage)
                    val isSystemUi = topPackage == "com.android.systemui" || 
                                     topPackage.contains("inputmethod") ||
                                     topPackage == "android"

                    if (isGame || isOurApp || isSystemUi) {
                        consecutiveAwaySeconds = 0
                    } else if (isLauncher) {
                        // User returned to device home screen / launcher
                        consecutiveAwaySeconds++
                        if (consecutiveAwaySeconds >= 2) {
                            withContext(Dispatchers.Main) {
                                closeAllAndStop()
                            }
                            break
                        }
                    } else {
                        // User opened or switched to another application
                        consecutiveAwaySeconds++
                        if (consecutiveAwaySeconds >= 3) {
                            withContext(Dispatchers.Main) {
                                closeAllAndStop()
                            }
                            break
                        }
                    }
                }
            }
        }
    }

    private fun getLauncherPackages(): Set<String> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfos.map { it.activityInfo.packageName }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun getForegroundPackage(usageStatsManager: UsageStatsManager?): String? {
        if (usageStatsManager == null || !PermissionManager.hasUsageStatsPermission(this)) {
            return null
        }
        return try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 3000L
            val events = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            var lastResumedPkg: String? = null
            var lastTimestamp = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp >= lastTimestamp) {
                    lastResumedPkg = event.packageName
                    lastTimestamp = event.timeStamp
                }
            }
            lastResumedPkg
        } catch (e: Exception) {
            null
        }
    }

    fun closeAllAndStop() {
        gameMonitorJob?.cancel()

        // 1. Tell MainActivity to finish if still alive
        try {
            val closeBroadcast = Intent(ACTION_CLOSE_ANTOS_APP).apply {
                setPackage(packageName)
            }
            sendBroadcast(closeBroadcast)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Stop screen capture / recording
        try {
            val stopCaptureIntent = Intent(this, ScreenCaptureService::class.java).apply {
                action = ScreenCaptureService.ACTION_STOP
            }
            startService(stopCaptureIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Stop monitoring
        try {
            monitoringManager.stopMonitoring()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Remove all overlay views from WindowManager
        hideAllOverlays()

        // 5. Cleanly stop foreground and service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    private fun hideAllOverlays() {
        composeView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            composeView = null
        }
        hideFpsOverlay()
    }


    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ANTOS Gaming Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        startForeground(NOTIFICATION_ID, buildNotification("Gaming mode is active"))
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ANTOS Gaming Core")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "CLOSE", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification(contentText))
    }

    private fun showOverlay() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            
            setContent {
                val fpsState by monitoringManager.fpsState.collectAsState()
                val temp by monitoringManager.temperature.collectAsState()
                val battery by monitoringManager.batteryLevel.collectAsState()
                val ram by monitoringManager.availableMemoryGb.collectAsState()
                val latency by monitoringManager.latency.collectAsState()
                val networkType by monitoringManager.networkType.collectAsState()

                AntosTheme {
                    OverlayComposeView(
                        fps = fpsState.fps,
                        minFps = fpsState.minimumFps,
                        maxFps = fpsState.maximumFps,
                        history = fpsState.history,
                        temp = temp,
                        battery = battery,
                        ram = ram,
                        latency = latency,
                        networkType = networkType,
                        onClose = { closeAllAndStop() },
                        updatePosition = { dx, dy -> updateOverlayPosition(dx, dy) }
                    )
                }
            }
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }

        windowManager.addView(composeView, layoutParams)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    private fun updateOverlayPosition(dx: Int, dy: Int) {
        layoutParams?.let { params ->
            params.x += dx
            params.y += dy
            windowManager.updateViewLayout(composeView, params)
        }
    }

    private var fpsComposeView: ComposeView? = null
    private var fpsLayoutParams: WindowManager.LayoutParams? = null

    private fun showFpsOverlay() {
        if (fpsComposeView != null) return
        
        fpsComposeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            
            setContent {
                val settingsManager = remember { com.antos.gaming.settings.SettingsManager(this@OverlayService) }
                val showNormal by settingsManager.showNormalFpsFlow.collectAsState(initial = false)
                val showAccurate by settingsManager.showAccurateFpsFlow.collectAsState(initial = false)
                val normalFpsState by monitoringManager.normalFpsState.collectAsState()
                val accurateFpsState by monitoringManager.accurateFpsState.collectAsState()
                val isAccurateActive = com.antos.gaming.monitoring.ScreenCaptureService.isFpsActive

                AntosTheme {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = androidx.compose.ui.Alignment.Center,
                        modifier = Modifier
                            .drawBehind {
                                val path = Path().apply {
                                    moveTo(10f, 0f)
                                    lineTo(size.width, 0f)
                                    lineTo(size.width, size.height - 10f)
                                    lineTo(size.width - 10f, size.height)
                                    lineTo(0f, size.height)
                                    lineTo(0f, 10f)
                                    close()
                                }
                                drawPath(path, color = com.antos.gaming.ui.theme.DarkPanel)
                                val borderColor = if (showAccurate && isAccurateActive) 
                                    com.antos.gaming.ui.theme.VioletMain 
                                else 
                                    com.antos.gaming.ui.theme.HudAccent
                                drawPath(path, color = borderColor.copy(alpha = 0.85f), style = Stroke(width = 1.dp.toPx()))
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    updateFpsOverlayPosition(dragAmount.x.toInt(), dragAmount.y.toInt())
                                }
                            }
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            if (showAccurate || isAccurateActive) {
                                androidx.compose.foundation.layout.Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "${accurateFpsState.fps} FPS",
                                        color = com.antos.gaming.ui.theme.VioletMain,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                    )
                                    androidx.compose.material3.Text(
                                        text = "ACCURATE",
                                        color = com.antos.gaming.ui.theme.HudAccent,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 7.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                            if (showNormal || (!showAccurate && !isAccurateActive)) {
                                androidx.compose.foundation.layout.Column(
                                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "${normalFpsState.fps} FPS",
                                        color = com.antos.gaming.ui.theme.HudAccent,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                                    )
                                    androidx.compose.material3.Text(
                                        text = "NORMAL",
                                        color = com.antos.gaming.ui.theme.VioletLight,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontSize = 7.sp,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        fpsLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 100
            y = 100
        }

        windowManager.addView(fpsComposeView, fpsLayoutParams)
    }

    private fun updateFpsOverlayPosition(dx: Int, dy: Int) {
        fpsLayoutParams?.let { params ->
            params.x -= dx // Because Gravity.END means x grows towards left
            params.y += dy
            windowManager.updateViewLayout(fpsComposeView, params)
        }
    }

    private fun hideFpsOverlay() {
        fpsComposeView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
            fpsComposeView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameMonitorJob?.cancel()
        monitoringManager.stopMonitoring()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        hideAllOverlays()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
