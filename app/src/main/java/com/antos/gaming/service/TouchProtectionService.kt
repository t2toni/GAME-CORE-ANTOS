package com.antos.gaming.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.antos.gaming.ui.theme.AntosTheme

class TouchProtectionService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        startForegroundService()
        showOverlay()
    }

    private fun startForegroundService() {
        val channelId = "antos_touch_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ANTOS Touch Protection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ANTOS Touch Protection")
            .setContentText("Touch protection is active")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()

        startForeground(3, notification)
    }

    private fun showOverlay() {
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@TouchProtectionService)
            setViewTreeViewModelStoreOwner(this@TouchProtectionService)
            setViewTreeSavedStateRegistryOwner(this@TouchProtectionService)
            
            setContent {
                AntosTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        stopSelf()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.layout.Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = com.antos.gaming.ui.theme.VioletMain,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = "DOUBLE TAP ANYWHERE TO UNLOCK",
                                color = Color.White,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            // FLAG_LAYOUT_IN_SCREEN ensures it covers the whole screen
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
        
        // Remove FLAG_NOT_FOCUSABLE to actually intercept touches, but keep it if we just want
        // to intercept touches in our view but still let back button work? 
        // Wait, if it's FLAG_NOT_FOCUSABLE, it intercepts touches on itself, but back button goes to underlying app.
        // That's exactly what we want for a simple touch lock. Actually, if we want to block ALL interaction, we might want it focusable.
        // Let's stick to FLAG_NOT_FOCUSABLE so system UI still works somewhat. Wait, if it's MATCH_PARENT, it consumes all touches anyway.

        windowManager.addView(composeView, layoutParams)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        composeView?.let {
            windowManager.removeView(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
}
